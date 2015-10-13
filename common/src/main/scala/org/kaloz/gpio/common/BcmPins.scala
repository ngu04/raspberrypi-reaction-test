package org.kaloz.gpio.common

import java.util.EnumSet

import com.pi4j.io.gpio
import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.pi4j.io.gpio.impl.PinImpl

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

object BcmPins {

  sealed abstract class DigitalPin(val address: Int, val name: String, val modes: EnumSet[PinMode] = EnumSet.of(PinMode.DIGITAL_INPUT, PinMode.DIGITAL_OUTPUT), val pwm: Boolean = false) {
    val underlying: gpio.Pin = new PinImpl(RaspiGpioProvider.NAME, address, name, modes, PinPullResistance.all)
  }

  sealed abstract class DigitalPwmPin(address: Int, name: String) extends DigitalPin(address, name, EnumSet.of(PinMode.DIGITAL_INPUT, PinMode.DIGITAL_OUTPUT, PinMode.PWM_OUTPUT), true)

  case class BCM_04(override val name: String) extends DigitalPin(7, name)

  case class BCM_17(override val name: String) extends DigitalPin(0, name)

  case class BCM_27(override val name: String) extends DigitalPin(2, name)

  case class BCM_22(override val name: String) extends DigitalPin(3, name)

  case class BCM_05(override val name: String) extends DigitalPin(21, name)

  case class BCM_06(override val name: String) extends DigitalPin(22, name)

  case class BCM_13(override val name: String) extends DigitalPwmPin(23, name)

  case class BCM_19(override val name: String) extends DigitalPwmPin(24, name)

  case class BCM_26(override val name: String) extends DigitalPin(25, name)

  case class BCM_18(override val name: String) extends DigitalPwmPin(1, name)

  case class BCM_23(override val name: String) extends DigitalPin(4, name)

  case class BCM_24(override val name: String) extends DigitalPin(5, name)

  case class BCM_25(override val name: String) extends DigitalPin(6, name)

  case class BCM_12(override val name: String) extends DigitalPwmPin(26, name)

  case class BCM_16(override val name: String) extends DigitalPin(27, name)

  case class BCM_20(override val name: String) extends DigitalPin(28, name)

  case class BCM_21(override val name: String) extends DigitalPin(29, name)

  implicit def convertToPin(pin: DigitalPin): gpio.Pin = pin.underlying

}

object BcmPinConversions {

  implicit class GPIOPinConversion(gpioPinInput: GpioPinInput) {
    def addStateChangeEventListener(eventHandler: GpioPinDigitalStateChangeEvent => Unit): GpioPinListenerDigital = {
      val gpioPinListenerDigital = new GpioPinListenerDigital {
        override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
          eventHandler(event)
        }
      }
      gpioPinInput.addListener(gpioPinListenerDigital)
      gpioPinListenerDigital
    }
  }
}
