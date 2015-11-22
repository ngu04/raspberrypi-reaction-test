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

  def bind()(implicit system: ActorSystem) = {
    implicit val flowMaterializer = ActorMaterializer()
    Http().bindAndHandle(route, "0.0.0.0", 8080)
  }

  private def route(implicit system: ActorSystem, materializer: ActorMaterializer) = {
    path("ws") {
      get {
        handleWebsocketMessages(webSocketActorFlow)
      }
    } ~ pathEndOrSingleSlash {
      getFromResource("www/index.html")
    } ~ pathPrefix("") {
      getFromResourceDirectory("www")
    }
  }

  private def webSocketActorFlow(implicit system: ActorSystem, materializer: ActorMaterializer): Flow[Message, Message, _] = {
    val webSocketActor = system.actorOf(Props[WebSocketActor])

    val in = Flow[Message].to(Sink.actorRef(webSocketActor, PoisonPill))
    val out = Source.actorRef(10, OverflowStrategy.fail).mapMaterializedValue(sink => webSocketActor ! Initialize(sink))
    Flow.wrap(in, out)(Keep.both)
  }

}
