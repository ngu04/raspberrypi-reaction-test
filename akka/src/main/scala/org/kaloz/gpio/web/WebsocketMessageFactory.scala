package org.kaloz.gpio.web

import akka.http.scaladsl.model.ws.TextMessage
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.kaloz.gpio.TestResult

object WebSocketMessageFactory {

  implicit val jsonFormat = org.json4s.DefaultFormats

  def registrationOpened = asTextMessage(("type" -> "registrationOpened"))
  def registrationClosed = asTextMessage(("type" -> "registrationClosed"))
  def leaderBoard(leaderBoard: List[TestResult]) = {
    asTextMessage(("type" -> "leaderBoard") ~ ("leaderBoard" -> leaderBoard.map(asJValue)))
  }

  private def asJValue(result: TestResult) = {
    ("name" -> result.user.name) ~ ("score" -> result.result.avg)
  }
  private def asTextMessage(json: JObject) = TextMessage(compact(render(json)))
}