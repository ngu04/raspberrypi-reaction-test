package org.kaloz.gpio

import java.util.UUID

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, SnapshotOffer}
import org.joda.time.DateTime
import org.kaloz.gpio.ReactionSessionControllerActor.StartTestSession
import org.kaloz.gpio.ReactionTestControllerActor.{ReactionTestResultArrivedEvent, SaveReactionTestResultCmd, TestAbortedEvent, TestEvent}
import org.kaloz.gpio.common.BcmPinConversions.GPIOPinConversion
import org.kaloz.gpio.common.BcmPins.{BCM_24, BCM_25}
import org.kaloz.gpio.common.PinController
import org.kaloz.gpio.web.WebSocketActor.RegistrationOpened

class ReactionTestControllerActor(pinController: PinController, reactionSessionControllerActor: ActorRef, numberOfWinners: Int) extends PersistentActor with ActorLogging {

  context.system.eventStream.subscribe(self, ReactionTestStateRequest.getClass)

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

  def updateState(evt: TestEvent): Unit = {
    evt match {
      case evt: ReactionTestResultArrivedEvent =>
        reactionTestState = reactionTestState.update(evt.testResult)
        saveSnapshot(reactionTestState)
        log.info(s"Result for ${evt.testResult.user.name} has been persisted!")
        log.info(s"${evt.testResult.result.iterations} iterations - ${evt.testResult.result.avg} ms avg response time - ${evt.testResult.result.std} std")
        log.info(s"Position with the best of the user is ${reactionTestState.positionOf(evt.testResult.user)}")
    }
    initializeDefaultButtons()
  }

  override def receiveCommand: Receive = LoggingReceive {
    case SaveReactionTestResultCmd(testResult) =>
      persist(ReactionTestResultArrivedEvent(testResult))(updateState)
      context.system.eventStream.publish(reactionTestState)
      context.system.eventStream.publish(RegistrationOpened)
    case TestAbortedEvent(userOption) =>
      userOption.fold(log.info(s"Test is aborted without user data..")) { user => log.info(s"Test is aborted for user $user") }
      initializeDefaultButtons()
    case ReactionTestStateRequest => sender ! reactionTestState
  }

  private def initializeDefaultButtons() = {
    log.info("Waiting test to be started!!")
    startButton.addStateChangeFallEventListener { event =>
      startButton.removeAllListeners()
      resultButton.removeAllListeners()
      reactionSessionControllerActor ! StartTestSession(self)
    }
    resultButton.addStateChangeFallEventListener { event =>
      log.info(s"Results!! ${reactionTestState.take(numberOfWinners)}")
      pinController.shutdown()
      context.system.terminate()
    }
  }
}

object ReactionTestControllerActor {
  def props(pinController: PinController, reactionSessionControllerActor: ActorRef, numberOfWinners: Int) = Props(classOf[ReactionTestControllerActor], pinController, reactionSessionControllerActor, numberOfWinners)

  case class SaveReactionTestResultCmd(testResult: TestResult)

  trait TestEvent

  case class TestAbortedEvent(user: Option[User]) extends TestEvent

  case class ReactionTestResultArrivedEvent(testResult: TestResult) extends TestEvent

}

case object ReactionTestStateRequest

case class ReactionTestState(testResults: List[TestResult] = List.empty) {
  def update(testResult: TestResult) = copy(testResult :: testResults)

  def take(numberOfWinners: Int) = testResults.groupBy(_.user).values.map(_.sorted.head).toList.sorted.take(numberOfWinners).zipWithIndex.map(_.swap)

  def bestOf(user: User) = testResults.filter(_.user == user).sorted.head

  def positionOf(user: User) = testResults.groupBy(_.user).values.map(_.sorted.head).toList.sorted.zipWithIndex.filter(item => item._1.user == user).head.swap
}

case class TestResult(user: User, result: Result) extends Ordered[TestResult] {
  def compare(that: TestResult): Int = result compare that.result
}

case class User(name: String, email: String, comments: Option[String])

case class Result(id: String = UUID.randomUUID().toString, startTime: DateTime = DateTime.now(), iterations: Int, avg: Int, std: Double) extends Ordered[Result] {
  def compare(that: Result): Int =
    (iterations compare that.iterations) match {
      case 0 =>
        (avg compare that.avg) match {
          case 0 => std compare that.std
          case c => c
        }
      case c => -c
    }
}

