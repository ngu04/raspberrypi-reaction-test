import sbt._

object Aliases {

  lazy val aliases = addCommandAlias("gpioTest", ";project gpio; ;clean ;runMain org.kaloz.gpio.GpioApp") ++
    addCommandAlias("akkaTest", ";project akka; ;clean ;runMain org.kaloz.gpio.GpioAkkaApp")
}
