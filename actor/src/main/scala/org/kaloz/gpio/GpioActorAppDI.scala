package org.kaloz.gpio

import akka.actor.ActorSystem
import org.kaloz.gpio.common.PinController
import org.kaloz.gpio.reaction.{ReactionTestControllerActor, ReactionTestSessionControllerActor, SingleLedReactionTestActor}
import org.kaloz.gpio.web.WebClientFactory

trait GpioActorAppDI extends GpioActorAppConfig {

  implicit val system = ActorSystem("gpio-akka", config)

  val pinController = new PinController()

  val singleLedReactionTest = system.actorOf(SingleLedReactionTestActor.props(pinController, reactionLedPulseLength), "singleLedReactionTest")
  val reactionTestSessionController = system.actorOf(ReactionTestSessionControllerActor.props(pinController, singleLedReactionTest, reactionThreshold, reactionLedPulseLength, wrongReactionPenalty), "reactionSessionController")
  val reactionTestController = system.actorOf(ReactionTestControllerActor.props(pinController, reactionTestSessionController), "reactionTestController")

  val binding = WebClientFactory.bind(reactionTestController, reactionTestSessionController)
}
