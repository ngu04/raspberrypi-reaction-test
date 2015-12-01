package org.kaloz.gpio

import scala.math.BigDecimal.RoundingMode

object ScoreTest extends App {


  val pulseLength = 1400
  val threshold = 30000

  val minus = threshold / pulseLength

  val iterations: Int = 27
  val average: Double = 1111
  val std: Double = 600

  val pure = if(iterations == minus) 0 else iterations + (1 / (average / 1000.0)) + (1 / (std / 100.0)) - minus

  println(BigDecimal(pure * 100).setScale(0, RoundingMode.HALF_UP))
}
