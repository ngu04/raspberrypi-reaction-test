package org.kaloz.gpio.common

import com.pi4j.io.gpio._
import com.typesafe.scalalogging.StrictLogging
import org.kaloz.gpio.common.BcmPins._

import scala.collection.JavaConversions._
import scala.languageFeature.postfixOps

class PinController extends StrictLogging {

  lazy val gpioController = GpioFactory.getInstance

  private def provision(digitalPin: DigitalPin, pinMode: PinMode)(provisioner: GpioController => GpioPin): GpioPin =
    gpioController.getProvisionedPins.find(_.getName == digitalPin.name) match {
      case None =>
        val pi4jPin = provisioner(gpioController)
        pi4jPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF)
        pi4jPin
      case Some(pi4jPin) if pi4jPin.getMode == pinMode => pi4jPin
      case Some(pi4jPin) => throw new IllegalArgumentException(s"${digitalPin.getName} is already provisioned!!")
    }

  def digitalInputPin(pin: DigitalPin, pinPullResistance: PinPullResistance = PinPullResistance.PULL_UP): GpioPinDigitalInput = {
    val pi4jPin = provision(pin, PinMode.DIGITAL_INPUT) {
      _.provisionDigitalInputPin(pin, pinPullResistance)
    }.asInstanceOf[GpioPinDigitalInput]
    pi4jPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF)
    pi4jPin
  }

  def digitalOutputPin(pin: DigitalPin, defaultState: PinState = PinState.LOW): GpioPinDigitalOutput =
    provision(pin, PinMode.DIGITAL_OUTPUT) {
      _.provisionDigitalOutputPin(pin, defaultState)
    }.asInstanceOf[GpioPinDigitalOutput]

  def digitalPwmOutputPin(pin: DigitalPwmPin, defaultValue: Int = 0): GpioPinPwmOutput = {
    val pi4jPin = provision(pin, PinMode.PWM_OUTPUT) {
      _.provisionPwmOutputPin(pin, defaultValue)
    }.asInstanceOf[GpioPinPwmOutput]
    pi4jPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF)
    pi4jPin
  }

  def shutdown(): Unit = {
    logger.info("Shutdown all provisioned pins...")
    gpioController.unprovisionPin(gpioController.getProvisionedPins.toSeq: _*)
    gpioController.shutdown()
  }

}
