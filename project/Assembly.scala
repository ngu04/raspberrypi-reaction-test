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

}
