package org.kaloz.gpio.rx

import org.kaloz.gpio.common.PinController

trait RxGpioAppDI extends RxGpioAppConfig {

  val pinController = new PinController()

}
