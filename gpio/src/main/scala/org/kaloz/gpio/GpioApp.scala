package org.kaloz.gpio

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.kaloz.gpio.ReactionController.ReactionTestState

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.Random
import scalaz.Scalaz._

object GpioApp extends App with GpioAppDI with StrictLogging {

  try {
    sessionHandler.runSession
  } catch {
    case x: Exception => logger.error("Error during reaction test:", x)
  } finally {
    gpioController.unprovisionPin(gpioController.getProvisionedPins.toSeq: _*)
    gpioController.shutdown()
  }

}

class SessionHandler(gpioController: GpioController, reactionController: ReactionController) extends StrictLogging {

  private val startButton = gpioController.provisionDigitalInputPin(RaspiBcmPin.BCM_25, "Start", PinPullResistance.PULL_UP)
  private val countDownLatch = new CountDownLatch(1)

  def startButtonTrigger: GpioPinListenerDigital = new GpioPinListenerDigital {
    override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
      logger.info("Reaction test session started!")

      startButton.removeAllListeners()
      reactionController.reactionTestStream.takeWhile(_.inProgress).foreach(x => logger.info(s"Reaction test result $x"))

      countDownLatch.countDown()
    }
  }

  def runSession = {
    logger.info("Reaction test is waiting to be started!")
    startButton.addListener(startButtonTrigger)
    countDownLatch.await()
    logger.info("Reaction test session stopped!")
  }

}

class ReactionController(gpioController: GpioController, reactionLedPulseLength: Int, reactionCorrectionFactor: Int, reactionThreshold: Int) extends StrictLogging {

  val WAIT_OFFSET_IN_MILLIS = 3000
  val WAIT_OFFSET_RANDOM_IN_MILLIS = 3000

  private val reactionLeds = List(gpioController.provisionDigitalOutputPin(RaspiBcmPin.BCM_19, "RedLed", PinState.LOW),
    gpioController.provisionDigitalOutputPin(RaspiBcmPin.BCM_13, "GreenLed", PinState.LOW))

  private val reactionButtons = List(gpioController.provisionDigitalInputPin(RaspiBcmPin.BCM_21, "RedLedButton", PinPullResistance.PULL_UP),
    gpioController.provisionDigitalInputPin(RaspiBcmPin.BCM_23, "GreenLedButton", PinPullResistance.PULL_UP))

  private val progressIndicatorLed = gpioController.provisionPwmOutputPin(RaspiBcmPin.BCM_12, "ProgressIndicatorLed", 0)
  private val stopButton = gpioController.provisionDigitalInputPin(RaspiBcmPin.BCM_24, "Stop", PinPullResistance.PULL_UP)

  private val counter = new AtomicInteger(0)

  def reactionTestStream() = {
    stopButton.addListener(stopButtonTrigger)

    Stream.continually {
      ReactionTestState(counter.incrementAndGet(), (runReactionTest |> verifyAggregatedResultBelowReactionThreshold) && notStopped)
    }
  }

  private def notStopped() = counter.get() >= 0

  private def verifyAggregatedResultBelowReactionThreshold(actualReactionProgress: Int) = actualReactionProgress < reactionThreshold

  private def runReactionTest() = {
    Thread.sleep(WAIT_OFFSET_IN_MILLIS + Random.nextInt(WAIT_OFFSET_RANDOM_IN_MILLIS))

    val reactionTestType = Random.nextInt(reactionLeds.size)
    val startTime = DateTime.now.getMillis

    logger.info(s"Reaction type is ${reactionLeds(reactionTestType).getName}!")

    Await.result(Future.firstCompletedOf(Seq(
      pulseTestLed(reactionTestType),
      buttonReaction(reactionTestType)
    )), reactionLedPulseLength * 2 millis)

    val reaction = (DateTime.now.getMillis - startTime).toInt
    logger.info(s"Reaction time is $reaction ms")

    progressIndicatorLed.setPwm(progressIndicatorLed.getPwm + reaction / reactionCorrectionFactor)
    progressIndicatorLed.getPwm
  }

  private def pulseTestLed(reactionTestType: Int): Future[Unit] = Future {
    reactionLeds(reactionTestType).pulse(reactionLedPulseLength, true)
    reactionButtons(reactionTestType).removeAllListeners()
  }

  private def buttonReaction(reactionTestType: Int): Future[Unit] = {
    val promise = Promise[Unit]

    reactionButtons(reactionTestType).addListener(new GpioPinListenerDigital {
      override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
        if (event.getState == PinState.HIGH) {
          reactionLeds(reactionTestType).setState(PinState.LOW)
          promise.success((): Unit)
        }
      }
    })

    promise.future
  }

  private def stopButtonTrigger: GpioPinListenerDigital = new GpioPinListenerDigital {
    override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
      stopButton.removeAllListeners()
      counter.set(Int.MinValue)
      logger.info("Reaction test session is interrupted!")
    }
  }
}

object ReactionController {

  case class ReactionTestState(numberOfTests: Int, inProgress: Boolean)

}
