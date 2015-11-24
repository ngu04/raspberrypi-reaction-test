import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._

object Assembly {

  lazy val actorAssemblySettings =
    Seq(
      mainClass in assembly := Some("org.kaloz.gpio.GpioActorApp"),
      mainClass in(Compile, run) := Some("org.kaloz.gpio.GpioActorApp"),
      assemblyJarName in assembly := "gpio-actor.jar"
    )

}
