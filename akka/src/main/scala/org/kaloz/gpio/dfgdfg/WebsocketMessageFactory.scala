package org.kaloz.gpio.dfgdfg

import akka.http.scaladsl.model.ws.TextMessage
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.kaloz.gpio.User

import scala.util.Random

object WebSocketMessageFactory {

  implicit val jsonFormat = org.json4s.DefaultFormats

  def registrationOpened = asTextMessage(("type" -> "registrationOpened"))
  def registrationClosed = asTextMessage(("type" -> "registrationClosed"))
  def leaderBoard = {
    val leaderBoard = List(User("name", "email", Some("sdfsdf")), User("name", "email", Some("sdfsdf")))
    asTextMessage(("type" -> "leaderBoard") ~ ("leaderBoard" -> leaderBoard.map(asJValue)))
  }

  private def asJValue(user: User) = {
    ("name" -> user.name) ~ ("score" -> (new Random()).nextInt())
  }
  private def asTextMessage(json: JObject) = TextMessage(compact(render(json)))
}