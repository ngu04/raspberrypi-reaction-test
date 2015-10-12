package org.kaloz.gpio.rx

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.language.implicitConversions

import rx.lang.scala._

object RxGpioApp extends App with RxGpioAppDI {

  val o = Observable.just(1, 2, 3)

  // Generally, we have two methods, `subscribe` and `foreach`, to listen to the messages from an Observable.
  // `foreach` is just an alias to `subscribe`.
//  o.subscribe(
//    n => println(n),
//    e => e.printStackTrace(),
//    () => println("done")
//  )

  o.foreach(
    n => println(n),
    e => e.printStackTrace(),
    () => println("done")
  )

  // For-comprehension is also an alternative, if you are only interested in `onNext`
  for (i <- o) {
    println(i)
  }

  val o2 = Observable.interval(200 millis).take(5)
  o2.subscribe(n => println("n = " + n))

  // need to wait here because otherwise JUnit kills the thread created by interval()
  waitFor(o2)

  println("done")

  def waitFor[T](obs: Observable[T]): Unit = {
    obs.toBlocking.toIterable.last
  }
}
