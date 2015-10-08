package org.kaloz.gpio

import org.kaloz.gpio.common.BcmPins.BCM_12
import org.kaloz.gpio.common.PinController

object PwmTest extends App {

  val pinController = new PinController()

  val led = pinController.digitalPwmOutputPin(BCM_12("led"))

  val stream = Stream.continually {
    Thread.sleep(500)
    led.setPwm(led.getPwm + 10)
    led.getPwm
  }

  stream.foreach(println(_))
}
