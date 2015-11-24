package org.kaloz.gpio.web

import akka.actor._
import akka.event.LoggingReceive
import akka.http.scaladsl.model.ws._
import akka.util.Timeout
import org.json4s._
import org.json4s.native.JsonMethods._
import org.kaloz.gpio.reaction.{ReactionTestControllerActor, ReactionTestSessionControllerActor}
import ReactionTestControllerActor._
import org.kaloz.gpio.reaction.ReactionTestSessionControllerActor
import ReactionTestSessionControllerActor._
import org.kaloz.gpio._
import org.kaloz.gpio.web.WebSocketActor._
import org.kaloz.gpio.web.WebSocketMessageFactory._

import scala.concurrent.duration._

class WebSocketActor(reactionTestController: ActorRef, reactionTestSessionController: ActorRef) extends Actor with ActorLogging {

  implicit val jsonFormat = org.json4s.DefaultFormats

  context.system.eventStream.subscribe(self, classOf[ReactionTestSessionStateChangedEvent])
  context.system.eventStream.subscribe(self, classOf[ReactionTestResultsUpdatedEvent])

  implicit val timeout = Timeout(5 seconds)

  def receive: Receive = LoggingReceive {
    case Initialize(sink) => {
      reactionTestController ! ReactionTestResultsRequest
      reactionTestSessionController ! ReactionTestSessionStateRequest
      context.become(receiveWithSink(sink))
    }
  }

  def receiveWithSink(sink: ActorRef): Receive = LoggingReceive {

    def updateLeaderBoard(testResults: List[TestResult]): Unit = sink ! leaderBoardStateMessage(testResults)

    def updateState(state: ReactionTestSessionState): Unit = state match {
      case WaitingReactionTestSessionStart => sink ! waitingStartSignalMessage
      case WaitingUserRegistration => sink ! openUserRegistrationMessage
      case WaitingSingleLedTestFinish => sink ! gameInProgressMessage
    }

    {
      case ReactionTestSessionStateChangedEvent(state) => updateState(state)
      case ReactionTestResultsUpdatedEvent(results) => updateLeaderBoard(results)

      case ReactionTestSessionStateResponse(state) => updateState(state)
      case ReactionTestResultsResponse(results) => updateLeaderBoard(results)

      case TextMessage.Strict(jsonString) => {
        val json = parse(jsonString)
        (json \ "type").extractOpt[String] map (_ match {
          case "user" => reactionTestSessionController ! AssignUserToReactionTestSessionCommand((json \ "user").extract[User])
        })
      }
    }
  }
}

object WebSocketActor {

  def props(reactionTestController: ActorRef, reactionTestSessionController: ActorRef) = Props(classOf[WebSocketActor], reactionTestController, reactionTestSessionController)

  case class Initialize(sink: ActorRef)

}