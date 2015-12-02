package org.kaloz

import java.util.UUID

import org.joda.time.DateTime
import org.kaloz.gpio.reaction.SingleLedReactionTestActor.{Missed, Reaction}

import scala.math.BigDecimal.RoundingMode

package object gpio {

  case class ReactionTestState(testResults: List[TestResult] = List.empty) {
    def update(testResult: TestResult) = copy(testResult :: testResults)
  }

  case class TestResult(user: User, result: Result)

  case class User(nickName: String, email: String, comments: Option[String])

  case class Result(iterations: Int, average: Int, std: Double, normalizer: Double, reactions: List[Reaction], id: String = UUID.randomUUID().toString, startTime: DateTime = DateTime.now()) {

    val score = {
      val pure = if (reactions.forall(_ == Missed) || iterations == Math.ceil(normalizer)) 0
      else iterations + (1 / (average / 1000.0)) + (1 / (std / 10.0)) - normalizer
      BigDecimal(pure * 100).setScale(0, RoundingMode.HALF_UP)
    }

    def numberOf(reaction: Reaction) = reactions.filter(_ == reaction).size

  }

}
