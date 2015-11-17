package org.kaloz.gpio

import akka.actor._
import akka.event.LoggingReceive
import com.pi4j.io.gpio.{GpioPinDigitalInput, GpioPinDigitalOutput, PinState}
import org.joda.time.DateTime
import org.kaloz.gpio.ReactionFlowControllerActor._
import org.kaloz.gpio.SessionHandlerActor.{SaveReactionTestResultCmd, TestAbortedEvent}
import org.kaloz.gpio.SingleLedReactionTestActor._
import org.kaloz.gpio.UserDataActor.GetUser
import org.kaloz.gpio.common.BcmPinConversions.GPIOPinConversion
import org.kaloz.gpio.common.BcmPins._
import org.kaloz.gpio.common.PinController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Random, Try}
import scalaz.Scalaz._

class ReactionFlowControllerActor(pinController: PinController, reactionLedPulseLength: Int, reactionCorrectionFactor: Int, reactionThreshold: Int) extends FSM[ReactionFlowState, FlowState] with ActorLogging {

  private val reactionLeds = List(BCM_19("RedLed"), BCM_13("GreenLed"), BCM_20("BlueLed")).map(pinController.digitalOutputPin(_))
  private val reactionButtons = List(BCM_21("RedLedButton"), BCM_23("GreenLedButton"), BCM_24("BlueLedButton")).map(pinController.digitalInputPin(_))
  private val progressIndicatorLed = pinController.digitalPwmOutputPin(BCM_12("ProgressIndicatorLed"))
  reactionButtons.foreach(_.setDebounce(2000))

  val singleLedReactionTestActor = context.system.actorOf(SingleLedReactionTestActor.props(reactionLedPulseLength))
  val userDataActor = context.system.actorOf(UserDataActor.props)
  val abortButton = pinController.digitalInputPin(BCM_25("AbortTest"))

  initialize()

  startWith(WaitingStartTestFlow, FlowState())

  when(WaitingStartTestFlow) {
    case Event(StartTestFlow, _) =>
      initFlow()
      goto(WaitingUserRegistration)
  }

  when(WaitingUserRegistration) {
    case Event(user: User, state: FlowState) =>
      startSingleLedTest()
      goto(WaitingSingleLedTestFinish) using state.withUser(user)
  }

  when(WaitingSingleLedTestFinish) {
    case Event(SingleLedReactionTestResult(reactionTime), state: FlowState) =>
      val newState = state.addReactionTime(reactionTime)
      val reactionPoints = newState.reactionTimes.map(_ / reactionCorrectionFactor).sum
      progressIndicatorLed.setPwm(reactionPoints)
      if (reactionPoints <= reactionThreshold) {
        startSingleLedTest()
        stay() using newState
      } else {
        stop(FSM.Normal) using newState
      }
    case Event(AbortTestFlow, _) => stop(FSM.Shutdown)
  }

  val removeListenerHandler: PartialFunction[StopEvent, StopEvent] = {
    case x => abortButton.removeAllListeners(); x
  }

  val terminationHandler: PartialFunction[StopEvent, Unit] = {
    case StopEvent(FSM.Normal, _, state@FlowState(Some(user), _, _, _, _)) =>
      context.parent ! SaveReactionTestResultCmd(TestResult(user, Result(iterations = state.numberOfTests, avg = state.averageReactionTime, std = state.standardDeviation)))
    case StopEvent(FSM.Shutdown, _, FlowState(Some(user), _, _, _, _)) =>
      context.parent ! TestAbortedEvent(user)
  }

  onTermination(removeListenerHandler.andThen(terminationHandler))

  def startSingleLedTest(): Unit = {
    val reactionTestType = Random.nextInt(reactionLeds.size)
    singleLedReactionTestActor ! StartSingleLedReactionTest(reactionLeds(reactionTestType), reactionButtons(reactionTestType))
  }

  def initFlow() = {
    progressIndicatorLed.setPwm(0)
    userDataActor ! GetUser
    abortButton.addStateChangeEventListener { event =>
      if (event.getState == PinState.LOW) {
        abortButton.removeAllListeners()
        self ! AbortTestFlow
      }
    }
  }
}

object ReactionFlowControllerActor {
  def props(pinController: PinController, reactionLedPulseLength: Int, reactionCorrectionFactor: Int, reactionThreshold: Int) = Props(classOf[ReactionFlowControllerActor], pinController, reactionLedPulseLength, reactionCorrectionFactor, reactionThreshold)

  sealed trait ReactionFlowState

  case object WaitingStartTestFlow extends ReactionFlowState

  case object WaitingUserRegistration extends ReactionFlowState

  case object WaitingSingleLedTestFinish extends ReactionFlowState

  sealed trait ReactionFlowMessage

  case object StartTestFlow extends ReactionFlowMessage

  case object AbortTestFlow extends ReactionFlowMessage

  case class FlowState(user: Option[User] = None, reactionTimes: List[Int] = List.empty) {

    def withUser(user: User) = copy(user = user.some)

    def addReactionTime(reactionTime: Int) = copy(reactionTimes = reactionTime :: reactionTimes)

    val averageReactionTime = Try(reactionTimes.sum / reactionTimes.size).getOrElse(0)

    val numberOfTests = reactionTimes.size

    val standardDeviation = math.sqrt(reactionTimes.foldLeft(0.0d)((total, item) => total + math.pow(item - averageReactionTime, 2)) / reactionTimes.size.toDouble)
  }

}

class SingleLedReactionTestActor(reactionLedPulseLength: Int) extends FSM[SingleTestState, TestState] with ActorLogging {

  private val WAIT_OFFSET_IN_MILLIS = 3000
  private val WAIT_OFFSET_RANDOM_IN_MILLIS = 3000

  initialize()

  startWith(WaitingStartSignal, TestState())

  when(WaitingStartSignal) {
    case Event(StartSingleLedReactionTest(ledPin, button), _) =>
      context.system.scheduler.scheduleOnce((WAIT_OFFSET_IN_MILLIS + Random.nextInt(WAIT_OFFSET_RANDOM_IN_MILLIS)) millis, self, SwitchOnLed)
      goto(WaitingRandomTimeExpiry) using TestState(ledPin.some, button.some)
  }

  when(WaitingRandomTimeExpiry) {
    case Event(SwitchOnLed, state@TestState(Some(ledPin), Some(button), None)) =>
      Future.sequence(Seq(pulseTestLedAndWait(ledPin), waitForUserReaction(button)))
      goto(WaitingUserReaction) using state.withStartTime()
  }

  when(WaitingUserReaction) {
    case Event(SwitchOffLed, TestState(Some(ledPin), Some(button), Some(startTime))) =>
      button.removeAllListeners()
      ledPin.setState(PinState.LOW)
      context.parent ! SingleLedReactionTestResult((DateTime.now.getMillis - startTime).toInt)
      goto(WaitingStartSignal) using TestState()
  }

  def pulseTestLedAndWait(ledPin: GpioPinDigitalOutput): Future[Unit] = Future {
    ledPin.pulse(reactionLedPulseLength, true)
    self ! SwitchOffLed
  }

  def waitForUserReaction(button: GpioPinDigitalInput): Future[Unit] = Future {
    button.addStateChangeEventListener { event =>
      if (event.getState == PinState.LOW) {
        self ! SwitchOffLed
      }
    }
  }
}

object SingleLedReactionTestActor {

  def props(reactionLedPulseLength: Int) = Props(classOf[SingleLedReactionTestActor], reactionLedPulseLength)

  sealed trait SingleTestState

  case object WaitingStartSignal extends SingleTestState

  case object WaitingRandomTimeExpiry extends SingleTestState

  case object WaitingUserReaction extends SingleTestState

  sealed trait SingleLedReactionTestMessage

  case class StartSingleLedReactionTest(ledPin: GpioPinDigitalOutput, button: GpioPinDigitalInput) extends SingleLedReactionTestMessage

  case object SwitchOnLed extends SingleLedReactionTestMessage

  case object SwitchOffLed extends SingleLedReactionTestMessage

  case class SingleLedReactionTestResult(reactionTime: Int) extends SingleLedReactionTestMessage

  case class TestState(ledPin: Option[GpioPinDigitalOutput] = None, button: Option[GpioPinDigitalInput] = None, startTime: Option[Long] = None) {
    def withStartTime() = copy(startTime = DateTime.now.getMillis.some)
  }

}

class UserDataActor extends Actor with ActorLogging {

  val users = List("krs", "tomi", "maki", "ima", "ilda", "bilda")

  override def receive: Receive = LoggingReceive {
    case GetUser => sender ! User(users(Random.nextInt(users.size)), "krs@krs.hu", "test")
  }
}

object UserDataActor {
  def props = Props[UserDataActor]

  case object GetUser

}
