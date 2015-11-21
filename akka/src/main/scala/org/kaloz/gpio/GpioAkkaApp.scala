package org.kaloz.gpio

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Await
import scala.concurrent.duration._

object GpioAkkaApp extends App with GpioAppDI with StrictLogging {

  //  implicit val mat = ActorMaterializer()(system)
  //  val readJournal = PersistenceQuery(system).readJournalFor[ScalaDslMongoReadJournal](MongoReadJournal.Identifier)
  //  val source = readJournal.currentEventsByPersistenceId(SessionHandlerActor.sessionHandlerActorPersistenceId, 0, Long.MaxValue)
  //  source.runForeach { event => logger.info("-------> Event: " + event) }

  logger.info("Waiting for termination...")
  Await.result(system.whenTerminated, Duration.Inf)

}