package org.kaloz.gpio

import java.util.EnumSet

import com.pi4j.io.gpio.impl.PinImpl
import com.pi4j.io.gpio.{Pin, PinMode, PinPullResistance, RaspiGpioProvider}

/**
 *
 * http://abyz.co.uk/rpi/pigpio/python.html#hardware_PWM
 *
 * +-----+-----+---------+------+---+---Pi 2---+---+------+---------+-----+-----+
 * | BCM | wPi |   Name  | Mode | V | Physical | V | Mode | Name    | wPi | BCM |
 * +-----+-----+---------+------+---+----++----+---+------+---------+-----+-----+
 * |     |     |    3.3v |      |   |  1 || 2  |   |      | 5v      |     |     |
 * |   2 |   8 |   SDA.1 |   IN | 1 |  3 || 4  |   |      | 5V      |     |     |
 * |   3 |   9 |   SCL.1 |   IN | 1 |  5 || 6  |   |      | 0v      |     |     |
 * |   4 |   7 | GPIO. 7 |   IN | 0 |  7 || 8  | 1 | ALT0 | TxD     | 15  | 14  |
 * |     |     |      0v |      |   |  9 || 10 | 1 | ALT0 | RxD     | 16  | 15  |
 * |  17 |   0 | GPIO. 0 |   IN | 0 | 11 || 12 | 0 | IN   | GPIO. 1 | 1   | 18  |
 * |  27 |   2 | GPIO. 2 |   IN | 0 | 13 || 14 |   |      | 0v      |     |     |
 * |  22 |   3 | GPIO. 3 |   IN | 0 | 15 || 16 | 0 | IN   | GPIO. 4 | 4   | 23  |
 * |     |     |    3.3v |      |   | 17 || 18 | 0 | IN   | GPIO. 5 | 5   | 24  |
 * |  10 |  12 |    MOSI |   IN | 0 | 19 || 20 |   |      | 0v      |     |     |
 * |   9 |  13 |    MISO |   IN | 0 | 21 || 22 | 0 | IN   | GPIO. 6 | 6   | 25  |
 * |  11 |  14 |    SCLK |   IN | 0 | 23 || 24 | 1 | IN   | CE0     | 10  | 8   |
 * |     |     |      0v |      |   | 25 || 26 | 1 | IN   | CE1     | 11  | 7   |
 * |   0 |  30 |   SDA.0 |   IN | 1 | 27 || 28 | 1 | IN   | SCL.0   | 31  | 1   |
 * |   5 |  21 | GPIO.21 |   IN | 0 | 29 || 30 |   |      | 0v      |     |     |
 * |   6 |  22 | GPIO.22 |   IN | 0 | 31 || 32 | 0 | IN   | GPIO.26 | 26  | 12  |
 * |  13 |  23 | GPIO.23 |   IN | 0 | 33 || 34 |   |      | 0v      |     |     |
 * |  19 |  24 | GPIO.24 |   IN | 0 | 35 || 36 | 0 | IN   | GPIO.27 | 27  | 16  |
 * |  26 |  25 | GPIO.25 |   IN | 0 | 37 || 38 | 0 | IN   | GPIO.28 | 28  | 20  |
 * |     |     |      0v |      |   | 39 || 40 | 0 | IN   | GPIO.29 | 29  | 21  |
 * +-----+-----+---------+------+---+----++----+---+------+---------+-----+-----+
 * | BCM | wPi |   Name  | Mode | V | Physical | V | Mode | Name    | wPi | BCM |
 * +-----+-----+---------+------+---+---Pi 2---+---+------+---------+-----+-----+
 */

object RaspiBcmPin {

  private val pinsMap: scala.collection.mutable.Map[String, Pin] = scala.collection.mutable.Map.empty

  val BCM_04: Pin = createDigitalPin(7, "BCM 4")
  val BCM_17: Pin = createDigitalPin(0, "BCM 17")
  val BCM_27: Pin = createDigitalPin(2, "BCM 27")
  val BCM_22: Pin = createDigitalPin(3, "BCM 22")
  val BCM_05: Pin = createDigitalPin(21, "BCM 5")
  val BCM_06: Pin = createDigitalPin(22, "BCM 6")
  val BCM_13: Pin = createDigitalAndPwmPin(23, "BCM 13")
  val BCM_19: Pin = createDigitalAndPwmPin(24, "BCM 19")
  val BCM_26: Pin = createDigitalPin(25, "BCM 26")

  val BCM_18: Pin = createDigitalAndPwmPin(1, "BCM 18")
  val BCM_23: Pin = createDigitalPin(4, "BCM 23")
  val BCM_24: Pin = createDigitalPin(5, "BCM 24")
  val BCM_25: Pin = createDigitalPin(6, "BCM 25")
  val BCM_12: Pin = createDigitalAndPwmPin(26, "BCM 12")
  val BCM_16: Pin = createDigitalPin(27, "BCM 16")
  val BCM_20: Pin = createDigitalPin(28, "BCM 20")
  val BCM_21: Pin = createDigitalPin(29, "BCM 21")

  private def createDigitalPin(address: Int, name: String): Pin = {
    createPin(address, name, EnumSet.of(PinMode.DIGITAL_INPUT, PinMode.DIGITAL_OUTPUT))
  }

  private def createDigitalAndPwmPin(address: Int, name: String): Pin = {
    createPin(address, name, EnumSet.of(PinMode.DIGITAL_INPUT, PinMode.DIGITAL_OUTPUT, PinMode.PWM_OUTPUT))
  }

  private def createPin(address: Int, name: String, modes: EnumSet[PinMode]): Pin = {
    val pin: Pin = new PinImpl(RaspiGpioProvider.NAME, address, name, modes, PinPullResistance.all)
    pinsMap + (name -> pin)
    pin
  }

  def pins = pinsMap.values

  def pinByName(name: String) = pinsMap.get(name)

}
