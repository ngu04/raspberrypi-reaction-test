package org.kaloz.gpio

import akka.actor._
import com.pi4j.io.gpio.{GpioPinDigitalInput, GpioPinDigitalOutput, PinState}
import org.joda.time.DateTime
import org.kaloz.gpio.ReactionSessionControllerActor._
import org.kaloz.gpio.ReactionTestControllerActor.{SaveReactionTestResultCmd, TestAbortedEvent}
import org.kaloz.gpio.SingleLedReactionTestActor._
import org.kaloz.gpio.common.BcmPinConversions.GPIOPinConversion
import org.kaloz.gpio.common.BcmPins._
import org.kaloz.gpio.common.PinController
import org.kaloz.gpio.web.WebSocketActor.{RegistrationClosed, RegistrationOpened}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Random, Try}
import scalaz.Scalaz._

class ReactionSessionControllerActor(pinController: PinController, singleLedReactionTestActor: ActorRef, reactionCorrectionFactor: Int, reactionThreshold: Int) extends FSM[ReactionFlowState, FlowStateData] with ActorLogging {

  private val reactionLeds = List(BCM_19("RedLed"), BCM_13("GreenLed"), BCM_20("BlueLed")).map(pinController.digitalOutputPin(_))
  private val reactionButtons = List(BCM_21("RedLedButton"), BCM_23("GreenLedButton"), BCM_24("BlueLedButton")).map(pinController.digitalInputPin(_))
  private val progressIndicatorLed = pinController.digitalPwmOutputPin(BCM_12("ProgressIndicatorLed"))
  reactionButtons.foreach(_.setDebounce(2000))

  private val abortButton = pinController.digitalInputPin(BCM_25("AbortTest"))

  startWith(WaitingStartTestFlow, FlowStateData())

  context.system.eventStream.subscribe(self, classOf[User])

  when(WaitingStartTestFlow) {
    case Event(StartTestSession(parent), state: FlowStateData) =>
      initFlow()
      goto(WaitingUserRegistration) using state.withParent(parent)
  }

  when(WaitingUserRegistration) {
    case Event(user: User, state: FlowStateData) =>
      log.info(s"User data arrived for $user")
      startSingleLedTest()
      context.system.eventStream.publish(RegistrationClosed)
      goto(WaitingSingleLedTestFinish) using state.withUser(user)
  }

  when(WaitingSingleLedTestFinish) {
    case Event(SingleLedReactionTestResult(reactionTime), state@FlowStateData(Some(user), _, Some(parent))) =>
      val newState = state.addReactionTime(reactionTime)
      val reactionPoints = newState.reactionTimes.map(_ / reactionCorrectionFactor).sum
      log.info(s"Current state: reactionTime -> $reactionTime ms, reactionPoints -> $reactionPoints, reactionThreshold -> $reactionThreshold")
      progressIndicatorLed.setPwm(reactionPoints)
      if (reactionPoints <= reactionThreshold) {
        startSingleLedTest()
        stay() using newState
      } else {
        parent ! SaveReactionTestResultCmd(TestResult(user, Result(iterations = newState.numberOfTests, avg = newState.averageReactionTime, std = newState.standardDeviation)))
        goto(WaitingStartTestFlow) using FlowStateData()
      }
  }

  onTransition {
    case x -> WaitingStartTestFlow => abortButton.removeAllListeners()
  }

  whenUnhandled {
    case Event(AbortTestFlow, FlowStateData(userOption, _, Some(parent))) =>
      parent ! TestAbortedEvent(userOption)
      singleLedReactionTestActor ! TerminateSingleLedReactionTest
      goto(WaitingStartTestFlow) using FlowStateData()
  }

  initialize()

  def startSingleLedTest(): Unit = {
    val reactionTestType = Random.nextInt(reactionLeds.size)
    singleLedReactionTestActor ! StartSingleLedReactionTest(reactionLeds(reactionTestType), reactionButtons(reactionTestType), self)
  }

  def initFlow() = {
    progressIndicatorLed.setPwm(0)
    log.info("Reaction test session started! Waiting for user data...")
    context.system.eventStream.publish(RegistrationOpened)
    abortButton.addStateChangeFallEventListener { event =>
      abortButton.removeAllListeners()
      self ! AbortTestFlow
    }
  }
}

object ReactionSessionControllerActor {
  def props(pinController: PinController, singleLedReactionTestActor: ActorRef, reactionCorrectionFactor: Int, reactionThreshold: Int) = Props(classOf[ReactionSessionControllerActor], pinController, singleLedReactionTestActor, reactionCorrectionFactor, reactionThreshold)

  sealed trait ReactionFlowState

  case object WaitingStartTestFlow extends ReactionFlowState

  case object WaitingUserRegistration extends ReactionFlowState

  case object WaitingSingleLedTestFinish extends ReactionFlowState

  sealed trait ReactionFlowMessage

  case class StartTestSession(parent: ActorRef) extends ReactionFlowMessage

  case object AbortTestFlow extends ReactionFlowMessage

  case class FlowStateData(user: Option[User] = None, reactionTimes: List[Int] = List.empty, parent: Option[ActorRef] = None) {

    def withUser(user: User) = copy(user = user.some)

    def withParent(parent: ActorRef) = copy(parent = parent.some)

    def addReactionTime(reactionTime: Int) = copy(reactionTimes = reactionTime :: reactionTimes)

    val averageReactionTime = Try(reactionTimes.sum / reactionTimes.size).getOrElse(0)

    val numberOfTests = reactionTimes.size

    val standardDeviation = math.sqrt(reactionTimes.foldLeft(0.0d)((total, item) => total + math.pow(item - averageReactionTime, 2)) / reactionTimes.size.toDouble)
  }

}

class SingleLedReactionTestActor(reactionLedPulseLength: Int) extends FSM[SingleTestState, TestStateData] with ActorLogging {

  private val WAIT_OFFSET_IN_MILLIS = 3000
  private val WAIT_OFFSET_RANDOM_IN_MILLIS = 3000

  startWith(WaitingStartSignal, TestStateData())

  when(WaitingStartSignal) {
    case Event(StartSingleLedReactionTest(ledPin, button, parent), _) =>
      val cancellable = context.system.scheduler.scheduleOnce((WAIT_OFFSET_IN_MILLIS + Random.nextInt(WAIT_OFFSET_RANDOM_IN_MILLIS)) millis, self, SwitchOnLed)
      goto(WaitingRandomTimeExpiry) using TestStateData(ledPin.some, button.some, cancellable.some, parent.some)
  }

  when(WaitingRandomTimeExpiry) {
    case Event(SwitchOnLed, state@TestStateData(Some(ledPin), Some(button), _, _, None)) =>
      Future.sequence(Seq(pulseTestLedAndWait(ledPin), waitForUserReaction(button)))
      goto(WaitingUserReaction) using state.withStartTime()
  }

  when(WaitingUserReaction) {
    case Event(SwitchOffLed, TestStateData(Some(ledPin), Some(button), _, Some(parent), Some(startTime))) =>
      button.removeAllListeners()
      ledPin.setState(PinState.LOW)
      parent ! SingleLedReactionTestResult((DateTime.now.getMillis - startTime).toInt)
      goto(WaitingStartSignal) using TestStateData()
  }

  whenUnhandled {
    case Event(TerminateSingleLedReactionTest, TestStateData(_, _, cancellable, _, _)) =>
      cancellable.foreach(_.cancel())
      goto(WaitingStartSignal) using TestStateData()
  }

  initialize()

  def pulseTestLedAndWait(ledPin: GpioPinDigitalOutput): Future[Unit] = Future {
    ledPin.pulse(reactionLedPulseLength, true)
    self ! SwitchOffLed
  }

  def waitForUserReaction(button: GpioPinDigitalInput): Future[Unit] = Future {
    button.addStateChangeFallEventListener { _ => self ! SwitchOffLed }
  }
}

object SingleLedReactionTestActor {

  def props(reactionLedPulseLength: Int) = Props(classOf[SingleLedReactionTestActor], reactionLedPulseLength)

  sealed trait SingleTestState

  case object WaitingStartSignal extends SingleTestState

  case object WaitingRandomTimeExpiry extends SingleTestState

  case object WaitingUserReaction extends SingleTestState

  sealed trait SingleLedReactionTestMessage

  case class StartSingleLedReactionTest(ledPin: GpioPinDigitalOutput, button: GpioPinDigitalInput, parent: ActorRef) extends SingleLedReactionTestMessage

  case object TerminateSingleLedReactionTest extends SingleLedReactionTestMessage

  case object SwitchOnLed extends SingleLedReactionTestMessage

  case object SwitchOffLed extends SingleLedReactionTestMessage

  case class SingleLedReactionTestResult(reactionTime: Int) extends SingleLedReactionTestMessage

  case class TestStateData(ledPin: Option[GpioPinDigitalOutput] = None, button: Option[GpioPinDigitalInput] = None, cancellable: Option[Cancellable] = None, parent: Option[ActorRef] = None, startTime: Option[Long] = None) {
    def withStartTime() = copy(cancellable = None, startTime = DateTime.now.getMillis.some)
  }

}