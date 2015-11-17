package org.kaloz.gpio

import akka.actor.ActorSystem
import org.kaloz.gpio.common.PinController

trait GpioAppDI extends GpioAppConfig {

  val system = ActorSystem("gpio-akka", config)

  val pinController = new PinController()

  val sessionHandlerActor = system.actorOf(SessionHandlerActor.props(pinController, reactionLedPulseLength, reactionCorrectionFactor, reactionThreshold, numberOfWinners), "sessionHandlerActor")

}
