import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._

object Assembly {

  lazy val gpioAssemblySettings =
  Seq(
    mainClass in assembly := Some("org.kaloz.gpio.GpioApp"),
    mainClass in (Compile, run) := Some("org.kaloz.gpio.GpioApp"),
    assemblyJarName in assembly := "gpio.jar"
  )

  lazy val akkaAssemblySettings =
    Seq(
      mainClass in assembly := Some("org.kaloz.gpio.GpioAkkaApp"),
      mainClass in(Compile, run) := Some("org.kaloz.gpio.GpioAkkaApp"),
      assemblyJarName in assembly := "gpio-akka.jar"
    )

}
