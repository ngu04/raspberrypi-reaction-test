package org.kaloz.gpio.reaction

import akka.actor._
import com.pi4j.io.gpio.{GpioPinDigitalInput, GpioPinDigitalOutput, PinState}
import org.joda.time.DateTime
import org.kaloz.gpio.common.BcmPinConversions.GPIOPinConversion
import org.kaloz.gpio.reaction.SingleLedReactionTestActor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random
import scalaz.Scalaz._

class SingleLedReactionTestActor(reactionLedPulseLength: Int) extends FSM[SingleLedTestState, SingleLedTestStateData] with ActorLogging {

  private val WAIT_OFFSET_IN_MILLIS = 3000
  private val WAIT_OFFSET_RANDOM_IN_MILLIS = 3000

  startWith(WaitingStartSignal, SingleLedTestStateData())

  when(WaitingStartSignal) {
    case Event(StartSingleLedReactionTestCommand(ledPin, button, parent), _) =>
      val cancellable = context.system.scheduler.scheduleOnce((WAIT_OFFSET_IN_MILLIS + Random.nextInt(WAIT_OFFSET_RANDOM_IN_MILLIS)) millis, self, SwitchOnLedCommand)
      goto(WaitingRandomTimeExpiry) using SingleLedTestStateData(ledPin.some, button.some, cancellable.some, parent.some)
  }

  when(WaitingRandomTimeExpiry) {
    case Event(SwitchOnLedCommand, state@SingleLedTestStateData(Some(ledPin), Some(button), _, _, None)) =>
      Future.sequence(Seq(pulseTestLedAndWait(ledPin), waitForUserReaction(button)))
      goto(WaitingUserReaction) using state.withStartTime()
  }

  when(WaitingUserReaction) {
    case Event(SwitchOffLedCommand, SingleLedTestStateData(Some(ledPin), Some(button), _, Some(parent), Some(startTime))) =>
      button.removeAllListeners()
      ledPin.setState(PinState.LOW)
      parent ! SingleLedReactionTestResult((DateTime.now.getMillis - startTime).toInt)
      goto(WaitingStartSignal) using SingleLedTestStateData()
  }

  whenUnhandled {
    case Event(TerminateSingleLedReactionTestCommand, SingleLedTestStateData(_, _, cancellable, _, _)) =>
      cancellable.foreach(_.cancel())
      goto(WaitingStartSignal) using SingleLedTestStateData()
  }

  initialize()

  def pulseTestLedAndWait(ledPin: GpioPinDigitalOutput): Future[Unit] = Future {
    ledPin.pulse(reactionLedPulseLength, true)
    self ! SwitchOffLedCommand
  }

  def waitForUserReaction(button: GpioPinDigitalInput): Future[Unit] = Future {
    button.addStateChangeFallEventListener { _ => self ! SwitchOffLedCommand }
  }
}

object SingleLedReactionTestActor {

  def props(reactionLedPulseLength: Int) = Props(classOf[SingleLedReactionTestActor], reactionLedPulseLength)

  sealed trait SingleLedTestState

  case object WaitingStartSignal extends SingleLedTestState

  case object WaitingRandomTimeExpiry extends SingleLedTestState

  case object WaitingUserReaction extends SingleLedTestState

  sealed trait SingleLedReactionTestMessage

  case class StartSingleLedReactionTestCommand(ledPin: GpioPinDigitalOutput, button: GpioPinDigitalInput, parent: ActorRef) extends SingleLedReactionTestMessage

  case object TerminateSingleLedReactionTestCommand extends SingleLedReactionTestMessage

  case object SwitchOnLedCommand extends SingleLedReactionTestMessage

  case object SwitchOffLedCommand extends SingleLedReactionTestMessage

  case class SingleLedReactionTestResult(reactionTime: Int) extends SingleLedReactionTestMessage

  case class SingleLedTestStateData(ledPin: Option[GpioPinDigitalOutput] = None, button: Option[GpioPinDigitalInput] = None, cancellable: Option[Cancellable] = None, parent: Option[ActorRef] = None, startTime: Option[Long] = None) {
    def withStartTime() = copy(cancellable = None, startTime = DateTime.now.getMillis.some)
  }

}
