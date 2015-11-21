import sbt.Keys._
import sbt._

object Version {

  val akka = "2.4.0"
  val jodaTime = "2.8.2"
  val scalazCore = "7.1.3"
  val scalaTest = "2.2.0"
  val config = "1.2.1"
  val mockito = "1.10.19"
  val macwire = "1.0.5"
  val pi4j = "1.0"
  val scalaLogging = "3.1.0"
  val logBack = "1.1.3"
  val rxScala = "0.25.0"
  val pi4jClient = "0.1.0-SNAPSHOT"
  val mongoPersitence = "0.7.6"
  val rxMongo = "1.0.10"
  val reactiveMongo = "0.11.7"
}

object Library {
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Version.akka
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.akka
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % Version.akka
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % Version.akka
  val akkaQuery = "com.typesafe.akka" %% "akka-persistence-query-experimental" % Version.akka
  val rxMongo = "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % Version.rxMongo
  val reactiveMongo = "org.reactivemongo" %% "reactivemongo" % Version.reactiveMongo
  val jodaTime = "joda-time" % "joda-time" % Version.jodaTime
  val config = "com.typesafe" % "config" % Version.config
  val scalazCore = "org.scalaz" %% "scalaz-core" % Version.scalazCore
  val mockito = "org.mockito" % "mockito-core" % Version.mockito
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
  val pi4jDevice = "com.pi4j" % "pi4j-device" % Version.pi4j
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % Version.scalaLogging
  val logBack = "ch.qos.logback" % "logback-classic" % Version.logBack
  val rxScala = "io.reactivex" %% "rxscala" % Version.rxScala
  val pi4jClientCore = "org.kaloz.pi4j.client" % "core" % Version.pi4jClient
  val pi4jClientRemote = "org.kaloz.pi4j.client" % "remote-client" % Version.pi4jClient

}

object Dependencies {

  import Library._

  val gpio = deps(
    config,
    jodaTime,
    scalazCore,
    pi4jClientRemote,
    pi4jDevice,
    scalaLogging,
    logBack,
    mockito % "test",
    akkaTestkit % "test",
    scalaTest % "test"
  )

  val akka = deps(
    config,
    jodaTime,
    akkaActor,
    akkaPersistence,
    akkaQuery,
    reactiveMongo,
    rxMongo,
    scalazCore,
    pi4jClientRemote,
    pi4jDevice,
    scalaLogging,
    logBack,
    mockito       	% "test",
    akkaTestkit     % "test",
    scalaTest     	% "test"
  )

  val common = deps(
    jodaTime,
    scalazCore,
    pi4jDevice,
    scalaLogging,
    logBack,
    mockito       	% "test",
    scalaTest     	% "test"
  )

  private def deps(modules: ModuleID*): Seq[Setting[_]] = Seq(libraryDependencies ++= modules)
}
