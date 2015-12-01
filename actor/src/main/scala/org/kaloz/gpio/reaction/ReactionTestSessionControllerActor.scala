package org.kaloz.gpio.reaction

import akka.actor._
import org.kaloz.gpio._
import org.kaloz.gpio.common.BcmPinConversions.GPIOPinConversion
import org.kaloz.gpio.common.BcmPins._
import org.kaloz.gpio.common.PinController
import org.kaloz.gpio.reaction.ReactionTestControllerActor.{ReactionTestAbortedEvent, SaveReactionTestResultCommand}
import org.kaloz.gpio.reaction.ReactionTestSessionControllerActor._
import org.kaloz.gpio.reaction.SingleLedReactionTestActor.{StartSingleLedReactionTestCommand, TerminateSingleLedReactionTestCommand, SingleLedReactionTestResult}

import scala.util.{Random, Try}
import scalaz.Scalaz._

class ReactionTestSessionControllerActor(pinController: PinController, singleLedReactionTestActor: ActorRef, reactionThreshold: Int, reactionLedPulseLength: Int) extends FSM[ReactionTestSessionState, ReactionTestSessionStateData] with ActorLogging {

  private val reactionLeds = List(BCM_19("RedLed"), BCM_13("GreenLed"), BCM_20("BlueLed")).map(pinController.digitalOutputPin(_))
  private val reactionButtons = List(BCM_21("RedLedButton"), BCM_23("GreenLedButton"), BCM_24("BlueLedButton")).map(pinController.digitalInputPin(_))
  private val progressIndicatorLed = pinController.digitalPwmOutputPin(BCM_12("ProgressIndicatorLed"))
  reactionButtons.foreach(_.setDebounce(2000))

  private val abortButton = pinController.digitalInputPin(BCM_25("AbortTest"))

  startWith(WaitingReactionTestSessionStart, ReactionTestSessionStateData())

  when(WaitingReactionTestSessionStart) {
    case Event(StartReactionTestSessionCommand(parent), state: ReactionTestSessionStateData) =>
      initSession()
      goto(WaitingUserRegistration) using state.withParent(parent)
  }

  when(WaitingUserRegistration) {
    case Event(AssignUserToReactionTestSessionCommand(user), state: ReactionTestSessionStateData) =>
      log.info(s"User data arrived for $user")
      startSingleLedTest()
      goto(WaitingSingleLedTestFinish) using state.withUser(user)
  }

  when(WaitingSingleLedTestFinish) {
    case Event(SingleLedReactionTestResult(reactionTime), state@ReactionTestSessionStateData(Some(user), _, Some(parent))) =>
      val newState = state.addReactionTime(reactionTime)
      val reactionPoints = newState.reactionTimes.sum
      log.info(s"Current state: reactionTime -> $reactionTime ms, reactionPoints -> $reactionPoints, reactionThreshold -> $reactionThreshold")
      progressIndicatorLed.setPwm(reactionPoints)
      if (reactionPoints <= reactionThreshold) {
        startSingleLedTest()
        stay() using newState
      } else {
        parent ! SaveReactionTestResultCommand(TestResult(user, Result(newState.numberOfTests, newState.averageReactionTime, newState.standardDeviation, reactionThreshold / reactionLedPulseLength)))
        abortButton.removeAllListeners()
        goto(WaitingReactionTestSessionStart) using ReactionTestSessionStateData()
      }
  }

  onTransition {
    case _ -> newState =>
      log.info(s"Publish new state $newState")
      context.system.eventStream.publish(ReactionTestSessionStateChangedEvent(newState))
  }

  whenUnhandled {
    case Event(AbortReactionTestSessionCommand, ReactionTestSessionStateData(userOption, _, Some(parent))) =>
      parent ! ReactionTestAbortedEvent(userOption)
      singleLedReactionTestActor ! TerminateSingleLedReactionTestCommand
      goto(WaitingReactionTestSessionStart) using ReactionTestSessionStateData()
    case Event(ReactionTestSessionStateRequest, _) =>
      log.debug(s"Current state is $stateName")
      sender ! ReactionTestSessionStateResponse(stateName)
      stay()
  }

  initialize()

  def startSingleLedTest(): Unit = {
    val reactionTestType = Random.nextInt(reactionLeds.size)
    singleLedReactionTestActor ! StartSingleLedReactionTestCommand(reactionLeds(reactionTestType), reactionButtons(reactionTestType), self)
  }

  def initSession() = {
    progressIndicatorLed.setPwm(0)
    log.info("Reaction test session started! Waiting for user data...")
    abortButton.addStateChangeFallEventListener { event =>
      abortButton.removeAllListeners()
      self ! AbortReactionTestSessionCommand
    }
  }
}

object ReactionTestSessionControllerActor {
  def props(pinController: PinController, singleLedReactionTestActor: ActorRef, reactionThreshold: Int, reactionLedPulseLength: Int) = Props(classOf[ReactionTestSessionControllerActor], pinController, singleLedReactionTestActor, reactionThreshold, reactionLedPulseLength)

  sealed trait ReactionTestSessionState

  case object WaitingReactionTestSessionStart extends ReactionTestSessionState

  case object WaitingUserRegistration extends ReactionTestSessionState

  case object WaitingSingleLedTestFinish extends ReactionTestSessionState

  sealed trait ReactionTestSessionMessage

  case class StartReactionTestSessionCommand(parent: ActorRef) extends ReactionTestSessionMessage

  case class AssignUserToReactionTestSessionCommand(user: User) extends ReactionTestSessionMessage

  case object AbortReactionTestSessionCommand extends ReactionTestSessionMessage

  case object ReactionTestSessionStateRequest extends ReactionTestSessionMessage

  case class ReactionTestSessionStateResponse(state: ReactionTestSessionState) extends ReactionTestSessionMessage

  case class ReactionTestSessionStateChangedEvent(state: ReactionTestSessionState)

  case class ReactionTestSessionStateData(user: Option[User] = None, reactionTimes: List[Int] = List.empty, parent: Option[ActorRef] = None) {

    def withUser(user: User) = copy(user = user.some)

    def withParent(parent: ActorRef) = copy(parent = parent.some)

    def addReactionTime(reactionTime: Int) = copy(reactionTimes = reactionTime :: reactionTimes)

    val averageReactionTime = Try(reactionTimes.sum / reactionTimes.size).getOrElse(0)

    val numberOfTests = reactionTimes.size

    val standardDeviation = math.sqrt(reactionTimes.foldLeft(0.0d)((total, item) => total + math.pow(item - averageReactionTime, 2)) / reactionTimes.size.toDouble)
  }

}