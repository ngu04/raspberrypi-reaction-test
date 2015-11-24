package org.kaloz.gpio

object ScoreTest extends App {

  var iter:Int = 5
  var avg:Double = 300
  var std:Double = 500.345

  var score = (iter * 1000) + (1000 * (1 / (avg / 1000))) + (100 * (1 / (std / 100)))

  println(score)
}
