package org.kaloz.gpio

import java.util.concurrent.CountDownLatch

import com.pi4j.io.gpio._
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.kaloz.gpio.ReactionFlowController.ReactionTestResult
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

class SessionHandler(pinController: PinController, reactionFlowController: ReactionFlowController) extends StrictLogging {

  private val countDownLatch = new CountDownLatch(1)

  def runSession = {
    logger.info("Reaction test is waiting to be started!")

    val startButton = pinController.digitalInputPin(BCM_25("Start"))
    startButton.addStateChangeEventListener { event =>
      logger.info("Reaction test session started!")
      startButton.removeAllListeners()
      val result = reactionFlowController.runReactionTest()
      logger.info(s"${result.averageReactionTime} ms avg response time in ${result.numberOfTests} tests")
      countDownLatch.countDown()
    }

    countDownLatch.await()
    logger.info("Reaction test session stopped!")
  }
}

class ReactionFlowController(pinController: PinController, reactionLedPulseLength: Int, reactionCorrectionFactor: Int, testEndThreshold: Int) extends StrictLogging {

  private val WAIT_OFFSET_IN_MILLIS = 3000
  private val WAIT_OFFSET_RANDOM_IN_MILLIS = 3000

  private val reactionLeds = List(BCM_19("RedLed"), BCM_13("GreenLed")).map(pinController.digitalOutputPin(_))
  private val reactionButtons = List(BCM_21("RedLedButton"), BCM_23("GreenLedButton")).map(pinController.digitalInputPin(_))
  private val progressIndicatorLed = pinController.digitalPwmOutputPin(BCM_12("ProgressIndicatorLed"))

  def runReactionTest(): ReactionTestResult = {
    reactionButtons.foreach(_.setDebounce(1000))
    val stopButton = pinController.digitalInputPin(BCM_24("Stop"))
    stopButton.addStateChangeEventListener { event =>
      stopButton.removeAllListeners()
      progressIndicatorLed.setPwm(testEndThreshold)
      logger.debug("Reaction test is interrupted!")
    }

    reactionTestStream().last
  }

  private def reactionTestStream(reactionTestResult: ReactionTestResult = ReactionTestResult()): Stream[ReactionTestResult] = {

    val counter = reactionTestResult.numberOfTests + 1

    def progressIndicatorValueBelowTestEndThreshold() = progressIndicatorLed.getPwm <= testEndThreshold

    def pulseTestLedAndWait(reactionTestType: Int): Future[Unit] = Future {
      reactionLeds(reactionTestType).pulse(reactionLedPulseLength, true)
      reactionButtons(reactionTestType).removeAllListeners()
      logger.debug(s"$counter. led switch off for ${reactionLeds(reactionTestType)}")
    }

    def waitForUserReaction(reactionTestType: Int): Future[Unit] = {
      val promise = Promise[Unit]

      logger.debug(s"$counter. start listener for ${reactionLeds(reactionTestType)}")
      reactionButtons(reactionTestType).addStateChangeEventListener { event =>
        logger.debug(s"$counter. button pushed")
        reactionLeds(reactionTestType).setState(PinState.LOW)
        promise.success((): Unit)
      }

      promise.future
    }

    def reactionTest() = {
      Thread.sleep(WAIT_OFFSET_IN_MILLIS + Random.nextInt(WAIT_OFFSET_RANDOM_IN_MILLIS))

      val reactionTestType = Random.nextInt(reactionLeds.size)
      val startTime = DateTime.now.getMillis

      logger.debug(s"$counter. Reaction type is ${reactionLeds(reactionTestType).getName}!")

      Await.result(Future.firstCompletedOf(Seq(
        pulseTestLedAndWait(reactionTestType),
        waitForUserReaction(reactionTestType)
      )), reactionLedPulseLength * 2 millis)

      val currentReactionTime = (DateTime.now.getMillis - startTime).toInt
      logger.info(s"$counter. Reaction time is $currentReactionTime ms")

      progressIndicatorLed.setPwm(progressIndicatorLed.getPwm + currentReactionTime / reactionCorrectionFactor)

      val currentTestResult = reactionTestResult.addReactionTime(currentReactionTime)
      currentTestResult #:: (if (progressIndicatorValueBelowTestEndThreshold()) reactionTestStream(currentTestResult) else Stream.empty )
    }

    logger.debug(s"$counter. current result $reactionTestResult")
    reactionTest()
  }

}

object ReactionFlowController {

  case class ReactionTestResult(reactionTimes: List[Int] = List.empty) {
    def addReactionTime(reactionTime: Int) = copy(reactionTime :: reactionTimes)

    def averageReactionTime = reactionTimes.sum / reactionTimes.size

    def numberOfTests = reactionTimes.size
  }

}