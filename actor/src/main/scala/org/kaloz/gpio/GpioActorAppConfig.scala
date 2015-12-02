package org.kaloz.gpio

import com.typesafe.config.ConfigFactory

trait GpioActorAppConfig {

  val config = ConfigFactory.load()

  val reactionLedPulseLength = config.getInt("reaction.led.pulse.length")
  val reactionThreshold = config.getInt("reaction.threshold")
  val wrongReactionPenalty = config.getInt("reaction.penalty")
}

