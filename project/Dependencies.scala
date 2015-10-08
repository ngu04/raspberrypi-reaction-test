import sbt._
import sbt.Keys._

object Version {

  val akka          = "2.3.12"
  val jodaTime      = "2.8.2"
  val scalazCore    = "7.1.3"
  val scalaTest     = "2.2.0"
  val config        = "1.2.1"
  val mockito       = "1.10.19"
  val macwire       = "1.0.5"
  val pi4j          = "1.0"
  val scalaLogging  = "3.1.0"
  val logBack       = "1.1.3"
}

object Library {
  val akkaActor       = "com.typesafe.akka"           %% "akka-actor"                    % Version.akka
  val akkaSlf4j       = "com.typesafe.akka"           %% "akka-slf4j"                    % Version.akka
  val akkaTestkit     = "com.typesafe.akka"           %% "akka-testkit"                  % Version.akka
  val jodaTime        = "joda-time"                   %  "joda-time"                     % Version.jodaTime
  val config          = "com.typesafe" 		            %  "config" 			                 % Version.config
  val scalazCore      = "org.scalaz"           	      %% "scalaz-core"                   % Version.scalazCore
  val mockito         = "org.mockito"                 %  "mockito-core"                  % Version.mockito
  val scalaTest       = "org.scalatest"               %% "scalatest"                     % Version.scalaTest
  val pi4jDevice      = "com.pi4j"                    %  "pi4j-device"                   % Version.pi4j
  val scalaLogging    = "com.typesafe.scala-logging"  %% "scala-logging"                 % Version.scalaLogging
  val logBack         = "ch.qos.logback"              % "logback-classic"                % Version.logBack
}

object Dependencies {

  import Library._

  val gpio = deps(
    akkaActor,
    akkaSlf4j,
    config,
    jodaTime,
    scalazCore,
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
