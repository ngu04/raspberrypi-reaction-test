package org.kaloz.gpio.rx

import com.typesafe.config.ConfigFactory

trait RxGpioAppConfig {
  private val config = ConfigFactory.load()

  val reactionLedPulseLength = config.getInt("reaction.led.pulse.length")
  val reactionCorrectionFactor = config.getInt("reaction.correction.factor")
  val reactionThreshold = config.getInt("reaction.threshold")
}

