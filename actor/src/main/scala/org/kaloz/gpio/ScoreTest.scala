package org.kaloz.gpio

import scala.math.BigDecimal.RoundingMode

object ScoreTest extends App {


  //13 iterations - 1089 ms avg response time - 292.79147213306294 std
  val pulseLength = 3000
  val threshold = 5000

  val minus = Math.ceil(threshold / pulseLength.toDouble)

  val iterations: Int =  3
  val average: Double =  (1 / (683 / 1000.0))
  val std: Double = (1 / (20.534523775 / 10.0))

  println(iterations)
  println(average)
  println(std)
  println(iterations + average + std)
  println(minus)
  val pure:BigDecimal = if(iterations == minus) 0 else BigDecimal(iterations + average + std) - minus

  println((pure * 100).setScale(0, RoundingMode.HALF_UP))
}
