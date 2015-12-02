package org.kaloz.gpio.reaction

import akka.actor._
import com.pi4j.io.gpio.PinState
import org.joda.time.DateTime
import org.kaloz.gpio.common.BcmPinConversions.GPIOPinConversion
import org.kaloz.gpio.common.BcmPins._
import org.kaloz.gpio.common.PinController
import org.kaloz.gpio.reaction.SingleLedReactionTestActor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random
import scalaz.Scalaz._

class SingleLedReactionTestActor(pinController: PinController, reactionLedPulseLength: Int) extends FSM[SingleLedTestState, SingleLedTestStateData] with ActorLogging {

  private val WAIT_OFFSET_IN_MILLIS = 3000
  private val WAIT_OFFSET_RANDOM_IN_MILLIS = 3000

  private val reactionLeds = List(BCM_19("RedLed"), BCM_13("GreenLed"), BCM_20("BlueLed")).map(pinController.digitalOutputPin(_))
  private val reactionButtons = List(BCM_21("RedLedButton"), BCM_23("GreenLedButton"), BCM_24("BlueLedButton")).map(pinController.digitalInputPin(_))
  reactionButtons.foreach(_.setDebounce(2800))

  startWith(WaitingStartSignal, SingleLedTestStateData())

  when(WaitingStartSignal) {
    case Event(StartSingleLedReactionTestCommand(parent), _) =>
      val reactionTestType = Random.nextInt(reactionLeds.size)
      val cancellable = context.system.scheduler.scheduleOnce((WAIT_OFFSET_IN_MILLIS + Random.nextInt(WAIT_OFFSET_RANDOM_IN_MILLIS)) millis, self, SwitchOnLedCommand)
      goto(WaitingRandomTimeExpiry) using SingleLedTestStateData(reactionTestType.some, cancellable.some, parent.some)
  }

  when(WaitingRandomTimeExpiry) {
    case Event(SwitchOnLedCommand, state@SingleLedTestStateData(Some(reactionTestType), _, _, None)) =>
      Future.sequence(Seq(pulseTestLedAndWait(reactionTestType), waitForUserReaction(reactionTestType), waitForBadUserReaction(reactionTestType)))
      goto(WaitingUserReaction) using state.withStartTime()
  }

  when(WaitingUserReaction) {
    case Event(SwitchOffLedCommand(reaction), SingleLedTestStateData(Some(reactionTestType), _, Some(parent), Some(startTime))) =>
      val result = DateTime.now.getMillis - startTime
      reactionButtons.foreach(_.removeAllListeners())
      reactionLeds(reactionTestType).setState(PinState.LOW)
      parent ! SingleLedReactionTestResult(result.toInt, reaction)
      goto(WaitingStartSignal) using SingleLedTestStateData()
  }

  whenUnhandled {
    case Event(TerminateSingleLedReactionTestCommand, SingleLedTestStateData(_, cancellable, _, _)) =>
      cancellable.foreach(_.cancel())
      goto(WaitingStartSignal) using SingleLedTestStateData()
  }

  initialize()

  def pulseTestLedAndWait(reactionTestType: Int): Future[Unit] = Future {
    reactionLeds(reactionTestType).pulse(reactionLedPulseLength, true)
    self ! SwitchOffLedCommand(Missed)
  }

  def waitForUserReaction(reactionTestType: Int): Future[Unit] = Future {
    reactionButtons(reactionTestType).addStateChangeFallEventListener { _ =>
      self ! SwitchOffLedCommand(Successful)
    }
  }

  def waitForBadUserReaction(reactionTestType: Int): Future[Unit] = Future {
    reactionButtons.filterNot(_ == reactionButtons(reactionTestType)).foreach { button =>
      button.addStateChangeFallEventListener { _ =>
        self ! SwitchOffLedCommand(Failure)
      }
    }
  }
}

object SingleLedReactionTestActor {

  def props(pinController: PinController, reactionLedPulseLength: Int) = Props(classOf[SingleLedReactionTestActor], pinController, reactionLedPulseLength)

  sealed trait SingleLedTestState

  case object WaitingStartSignal extends SingleLedTestState

  case object WaitingRandomTimeExpiry extends SingleLedTestState

  case object WaitingUserReaction extends SingleLedTestState

  sealed trait SingleLedReactionTestMessage

  case class StartSingleLedReactionTestCommand(parent: ActorRef) extends SingleLedReactionTestMessage

  case object TerminateSingleLedReactionTestCommand extends SingleLedReactionTestMessage

  case object SwitchOnLedCommand extends SingleLedReactionTestMessage

  sealed trait Reaction

  case object Successful extends Reaction

  case object Failure extends Reaction

  case object Missed extends Reaction

  case class SwitchOffLedCommand(reaction: Reaction) extends SingleLedReactionTestMessage

  case class SingleLedReactionTestResult(reactionTime: Int, reaction: Reaction) extends SingleLedReactionTestMessage

  case class SingleLedTestStateData(reactionTestType: Option[Int] = None, cancellable: Option[Cancellable] = None, parent: Option[ActorRef] = None, startTime: Option[Long] = None) {
    def withStartTime() = copy(cancellable = None, startTime = DateTime.now.getMillis.some)
  }

}
