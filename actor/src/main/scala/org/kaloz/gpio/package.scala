package org.kaloz

import java.util.UUID

import org.joda.time.DateTime

package object gpio {

  case class ReactionTestState(testResults: List[TestResult] = List.empty) {
    def update(testResult: TestResult) = copy(testResult :: testResults)
  }

  case class TestResult(user: User, result: Result)

  case class User(name: String, email: String, comments: Option[String])

  case class Result(id: String = UUID.randomUUID().toString, startTime: DateTime = DateTime.now(), iterations: Int, average: Int, std: Double) {
    val score = (iterations * 1000) + (1000 * (1 / (average / 1000))) + (100 * (1 / (std / 100)))
  }

}
