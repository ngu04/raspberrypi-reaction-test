package org.kaloz.gpio

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, SnapshotOffer}
import org.kaloz.gpio.ReactionTestControllerActor._
import org.kaloz.gpio.ReactionTestSessionControllerActor.StartReactionTestSessionCommand
import org.kaloz.gpio.common.BcmPinConversions.GPIOPinConversion
import org.kaloz.gpio.common.BcmPins.{BCM_24, BCM_25}
import org.kaloz.gpio.common.PinController

class ReactionTestControllerActor(pinController: PinController, reactionTestSessionController: ActorRef) extends PersistentActor with ActorLogging {

  override val persistenceId: String = "reactionTestControllerPersistenceId"

  val startButton = pinController.digitalInputPin(BCM_25("Start"))
  val resultButton = pinController.digitalInputPin(BCM_24("Result"))

  var reactionTestState = ReactionTestState()

  initializeDefaultButtons()

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, offeredSnapshot: ReactionTestState) =>
      reactionTestState = offeredSnapshot
      log.info(s"Snapshot has been loaded with ${reactionTestState.testResults.size} test results!")
  }

  def updateState(evt: ReactionTestResultArrivedEvent): Unit = {
    reactionTestState = reactionTestState.update(evt.testResult)
    saveSnapshot(reactionTestState)

    log.info(s"Result for ${evt.testResult.user.name} has been persisted!")
    log.info(s"${evt.testResult.result.iterations} iterations - ${evt.testResult.result.average} ms avg response time - ${evt.testResult.result.std} std")

    context.system.eventStream.publish(ReactionTestResultsUpdatedEvent(reactionTestState.testResults))
    initializeDefaultButtons()
  }

  override def receiveCommand: Receive = LoggingReceive {
    case SaveReactionTestResultCommand(testResult) => persist(ReactionTestResultArrivedEvent(testResult))(updateState)
    case ReactionTestAbortedEvent(userOption) =>
      userOption.fold(log.info(s"Test is aborted without user data..")) { user => log.info(s"Test is aborted for user $user") }
      initializeDefaultButtons()
    case ReactionTestResultsRequest => sender ! ReactionTestResultsResponse(reactionTestState.testResults)
  }

  private def initializeDefaultButtons() = {
    log.info("Waiting test to be started!!")
    startButton.addStateChangeFallEventListener { event =>
      startButton.removeAllListeners()
      resultButton.removeAllListeners()
      reactionTestSessionController ! StartReactionTestSessionCommand(self)
    }
    resultButton.addStateChangeFallEventListener { event =>
      log.info("Shutdown...")
      pinController.shutdown()
      context.system.terminate()
    }
  }
}

object ReactionTestControllerActor {
  def props(pinController: PinController, reactionTestSessionController: ActorRef) = Props(classOf[ReactionTestControllerActor], pinController, reactionTestSessionController)

  case class SaveReactionTestResultCommand(testResult: TestResult)

  case object ReactionTestResultsRequest

  case class ReactionTestResultsResponse(testResults: List[TestResult])

  case class ReactionTestResultArrivedEvent(testResult: TestResult)

  case class ReactionTestResultsUpdatedEvent(testResults: List[TestResult])

  case class ReactionTestAbortedEvent(user: Option[User])

}
