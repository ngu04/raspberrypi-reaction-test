package org.kaloz.gpio

import akka.actor.ActorSystem
import org.kaloz.gpio.common.PinController
import org.kaloz.gpio.web.WebClientFactory

trait GpioAppDI extends GpioAppConfig {

  implicit val system = ActorSystem("gpio-akka", config)

  val pinController = new PinController()

  val singleLedReactionTestActor = system.actorOf(SingleLedReactionTestActor.props(reactionLedPulseLength), "singleLedReactionTestActor")
  val reactionSessionControllerActor = system.actorOf(ReactionSessionControllerActor.props(pinController, singleLedReactionTestActor, reactionCorrectionFactor, reactionThreshold), "reactionSessionControllerActor")
  system.actorOf(ReactionTestControllerActor.props(pinController, reactionSessionControllerActor, numberOfWinners), "reactionTestControllerActor")

  val binding = WebClientFactory.bind()
}
