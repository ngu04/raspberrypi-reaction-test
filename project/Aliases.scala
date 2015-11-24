import sbt._

object Aliases {

  lazy val aliases = addCommandAlias("gpioActorTest", ";project actor; ;clean ;runMain org.kaloz.gpio.GpioActorApp")
}
