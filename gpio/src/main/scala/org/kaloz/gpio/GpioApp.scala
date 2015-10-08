package org.kaloz.gpio

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

import com.pi4j.io.gpio._
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.kaloz.gpio.ReactionController.ReactionTestState
import org.kaloz.gpio.common.BcmPinConversions.GPIOPinConversion
import org.kaloz.gpio.common.BcmPins._
import org.kaloz.gpio.common.PinController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Random, Try}

object GpioApp extends App with GpioAppDI with StrictLogging {

  Try(sessionHandler.runSession).recover { case x: Exception => logger.error("Error during reaction test:", x) }.map(_ => pinController.shutdown())

}

class SessionHandler(pinController: PinController, reactionController: ReactionController) extends StrictLogging {

  private val startButton = pinController.digitalInputPin(BCM_25("Start"))
  private val countDownLatch = new CountDownLatch(1)

  def runSession = {
    logger.info("Reaction test is waiting to be started!")

    startButton.addStateChangeEventListener { event =>
      logger.info("Reaction test session started!")
      startButton.removeAllListeners()
      val (sum, numberOfTries) = reactionController.reactionTestStream.takeWhile(_.inProgress).foldLeft((0, 0))((sum, result) => (sum._1 + result.reaction, result.numberOfTests))
      logger.info(s"${sum / numberOfTries} ms avg response")
      countDownLatch.countDown()
    }

    countDownLatch.await()
    logger.info("Reaction test session stopped!")
  }

}

class ReactionController(pinController: PinController, reactionLedPulseLength: Int, reactionCorrectionFactor: Int, reactionThreshold: Int) extends StrictLogging {

  val WAIT_OFFSET_IN_MILLIS = 3000
  val WAIT_OFFSET_RANDOM_IN_MILLIS = 3000

  private val reactionLeds = List(BCM_19("RedLed"), BCM_13("GreenLed")).map(pinController.digitalOutputPin(_))
  private val reactionButtons = List(BCM_21("RedLedButton"), BCM_23("GreenLedButton")).map(pinController.digitalInputPin(_))
  reactionButtons.foreach(_.setDebounce(1000))

  private val progressIndicatorLed = pinController.digitalPwmOutputPin(BCM_12("ProgressIndicatorLed"))
  private val stopButton = pinController.digitalInputPin(BCM_24("Stop"))

  private val counter = new AtomicInteger(1)

  def reactionTestStream() = {
    stopButton.addStateChangeEventListener { event =>
      stopButton.removeAllListeners()
      counter.set(Int.MinValue)
      logger.info(s"$counter Reaction test session is interrupted!")
    }

    Stream.continually {
      val (reaction, actualReactionProgress) = runReactionTest
      ReactionTestState(counter.getAndIncrement(), reaction, verifyAggregatedResultBelowReactionThreshold(actualReactionProgress) && notStopped)
    }
  }

  private def notStopped() = counter.get() >= 0

  private def verifyAggregatedResultBelowReactionThreshold(actualReactionProgress: Int) = actualReactionProgress < reactionThreshold

  private def runReactionTest() = {
    Thread.sleep(WAIT_OFFSET_IN_MILLIS + Random.nextInt(WAIT_OFFSET_RANDOM_IN_MILLIS))

    val reactionTestType = Random.nextInt(reactionLeds.size)
    val startTime = DateTime.now.getMillis

    logger.info(s"$counter Reaction type is ${reactionLeds(reactionTestType).getName}!")

    Await.result(Future.firstCompletedOf(Seq(
      pulseTestLed(reactionTestType),
      buttonReaction(reactionTestType)
    )), reactionLedPulseLength * 2 millis)

    val reaction = (DateTime.now.getMillis - startTime).toInt
    logger.info(s"$counter Reaction time is $reaction ms")

    progressIndicatorLed.setPwm(progressIndicatorLed.getPwm + reaction / reactionCorrectionFactor)
    (reaction, progressIndicatorLed.getPwm)
  }

  private def pulseTestLed(reactionTestType: Int): Future[Unit] = Future {
    reactionLeds(reactionTestType).pulse(reactionLedPulseLength, true)
    reactionButtons(reactionTestType).removeAllListeners()
    logger.info(s"$counter led switch off for ${reactionLeds(reactionTestType)}")
  }

  private def buttonReaction(reactionTestType: Int): Future[Unit] = {
    val promise = Promise[Unit]

    logger.info(s"$counter start listener for ${reactionLeds(reactionTestType)}")
    reactionButtons(reactionTestType).addStateChangeEventListener { event =>
      logger.info(s"$counter button pushed")
      reactionLeds(reactionTestType).setState(PinState.LOW)
      promise.success((): Unit)
    }

    promise.future
  }
}

object ReactionController {

  case class ReactionTestState(numberOfTests: Int, reaction: Int, inProgress: Boolean)

}
