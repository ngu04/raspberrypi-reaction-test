package org.kaloz.gpio.web

import akka.actor._
import akka.http.scaladsl.model.ws._
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import org.json4s._
import org.json4s.native.JsonMethods._
import org.kaloz.gpio.{ReactionTestStateRequest, ReactionTestState, User}
import org.kaloz.gpio.web.WebSocketActor.{RegistrationClosed, Initialize, RegistrationOpened}
import org.kaloz.gpio.web.WebSocketMessageFactory._

import scala.concurrent.duration._

class WebSocketActor extends Actor with StrictLogging {

  implicit val jsonFormat = org.json4s.DefaultFormats
  context.system.eventStream.subscribe(self, RegistrationOpened.getClass)
  context.system.eventStream.subscribe(self, RegistrationClosed.getClass)
  context.system.eventStream.subscribe(self, classOf[ReactionTestState])

  implicit val timeout = Timeout(5 seconds)

  def receive = {
    case Initialize(sink) => {
      context.system.eventStream.publish(ReactionTestStateRequest)
      context.become(receiveWithSink(sink))
    }
  }

  def receiveWithSink(sink: ActorRef): Receive = {
    case RegistrationOpened => sink ! registrationOpened
    case RegistrationClosed => sink ! registrationClosed
    case ReactionTestState(results) => sink ! leaderBoard(results)

    case TextMessage.Strict(jsonString) => {
      val json = parse(jsonString)
      (json \ "type").extractOpt[String] map (_ match {
        case "user" =>  context.system.eventStream.publish((json \ "user").extract[User])
      })
    }
  }
}

object WebSocketActor {

  def props = Props[WebSocketActor]

  case class Initialize(sink: ActorRef)

  case object RegistrationOpened

  case object RegistrationClosed

}