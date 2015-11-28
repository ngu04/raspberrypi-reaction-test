import net.virtualvoid.sbt.graph.Plugin
import sbt.Keys._
import sbt._

object BaseSettings {

  lazy val javaagent = "-javaagent:" + System.getProperty("user.home") + "/.ivy2/cache/org.aspectj/aspectjweaver/jars/aspectjweaver-1.8.7.jar"

  lazy val settings =
  Seq(
    version := "1.0.0",
    organization := "org.kaloz.gpio",
    description := "Gpio Project",
    scalaVersion := "2.11.6",
    homepage := Some(url("http://kaloz.org")),
    scalacOptions := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.8",
      "-language:postfixOps",
      "-language:implicitConversions"
    ),
    javacOptions := Seq(
      "-Xlint:unchecked", 
      "-Xlint:deprecation"
    ),
    shellPrompt := { s => "[" + scala.Console.BLUE + Project.extract(s).currentProject.id + scala.Console.RESET + "] $ "}
  ) ++
    Resolvers.settings ++
  Testing.settings ++
  Plugin.graphSettings ++
  Aliases.aliases

  //Required by Aspects
  lazy val javaagentSettings = settings ++ Seq(
    javaOptions in run ++= Seq(javaagent, "-Dpi4j.client.mode=remote", "-Dakka.cluster.seed-nodes.0=akka.tcp://pi4j-remoting@192.168.1.110:2552"),
    fork in run := true
  )

}
