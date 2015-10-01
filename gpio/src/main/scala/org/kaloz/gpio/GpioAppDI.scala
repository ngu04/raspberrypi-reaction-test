package org.kaloz.gpio

import com.pi4j.io.gpio.GpioFactory


trait GpioAppDI extends GpioAppConfig {

  val gpioController = GpioFactory.getInstance()

  val reactionController = new ReactionController(gpioController, reactionLedPulseLength, reactionCorrectionFactor, reactionThreshold)
  val sessionHandler = new SessionHandler(gpioController, reactionController)
}
