package org.kaloz.gpio.rx

import java.util.concurrent.CountDownLatch

import com.pi4j.io.gpio.PinState
import com.typesafe.scalalogging.StrictLogging
import org.kaloz.gpio.common.BcmPins.BCM_25

import org.kaloz.gpio.common.BcmPinConversions._
import scala.language.postfixOps
import scala.language.implicitConversions

import rx.lang.scala._

object RxGpioApp extends App with RxGpioAppDI with StrictLogging {

  logger.info("Start")

  val countDownLatch = new CountDownLatch(1)

  val buttonObservable = Observable.create[PinState]({observer =>
    val button = pinController.digitalInputPin(BCM_25("testButton"))
    val listener = button.addStateChangeEventListener { event =>
      observer.onNext(event.getState)
    }
    Subscription{
      logger.info("Button unsubscribed...")
      button.removeListener(listener)
    }
  }).take(4).last

  val buttonSubscription = buttonObservable.subscribe(
    n => println(n),
    e => e.printStackTrace(),
    () => countDownLatch.countDown()
  )

  countDownLatch.await()

  buttonSubscription.unsubscribe()

  pinController.shutdown()
  logger.info("Exit")

}
