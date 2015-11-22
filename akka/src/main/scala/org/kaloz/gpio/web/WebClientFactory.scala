package org.kaloz.gpio.web

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.typesafe.scalalogging.StrictLogging
import org.kaloz.gpio.web.WebSocketActor.Initialize

import scala.concurrent.ExecutionContext.Implicits.global

object WebClientFactory extends StrictLogging {

  def bind(reactionTestController: ActorRef, reactionTestSessionController: ActorRef)(implicit system: ActorSystem) = {
    implicit val flowMaterializer = ActorMaterializer()
    Http().bindAndHandle(route(reactionTestController, reactionTestSessionController), "0.0.0.0", 8080)
  }

  private def route(reactionTestController: ActorRef, reactionTestSessionController: ActorRef)(implicit system: ActorSystem, materializer: ActorMaterializer) = {
    path("ws") {
      get {
        handleWebsocketMessages(webSocketActorFlow(reactionTestController, reactionTestSessionController))
      }
    } ~ pathEndOrSingleSlash {
      getFromResource("www/index.html")
    } ~ pathPrefix("") {
      getFromResourceDirectory("www")
    }
  }

  private def webSocketActorFlow(reactionTestController: ActorRef, reactionTestSessionController: ActorRef)(implicit system: ActorSystem, materializer: ActorMaterializer): Flow[Message, Message, _] = {
    val webSocketActor = system.actorOf(WebSocketActor.props(reactionTestController, reactionTestSessionController))

    val in = Flow[Message].to(Sink.actorRef(webSocketActor, PoisonPill))
    val out = Source.actorRef(10, OverflowStrategy.fail).mapMaterializedValue(sink => webSocketActor ! Initialize(sink))
    Flow.wrap(in, out)(Keep.both)
  }

}
