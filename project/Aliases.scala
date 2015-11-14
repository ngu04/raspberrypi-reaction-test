import sbt._

object Aliases {

  lazy val aliases = addCommandAlias("runTest", ";project gpio; ;clean ;runMain org.kaloz.gpio.GpioApp")
}
