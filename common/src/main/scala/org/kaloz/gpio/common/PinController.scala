package org.kaloz.gpio.common

import com.pi4j.io.gpio._
import com.typesafe.scalalogging.StrictLogging
import org.kaloz.gpio.common.BcmPins._

import scala.collection.JavaConversions._
import scala.languageFeature.postfixOps
import scalaz.Scalaz._

class PinController extends StrictLogging {

  lazy val gpioController = GpioFactory.getInstance

  private def provision(digitalPin: DigitalPin, pinMode: PinMode)(provisioner: GpioController => GpioPin): GpioPin =
    gpioController.getProvisionedPins.find(_.getName == digitalPin.name) match {
      case None =>
        val pi4jPin = provisioner(gpioController)
        logger.info(s"Provisioning ${digitalPin.getClass.getSimpleName} - ${digitalPin.name}")
        pi4jPin
      case Some(pi4jPin) if pi4jPin.getMode == pinMode => pi4jPin
      case Some(pi4jPin) => throw new IllegalArgumentException(s"${digitalPin.getName} is already provisioned with different pinMode - ${pi4jPin.getMode}!!")
    }

  private def setShutdownOptions(gpioPin: GpioPin): GpioPin = {
    gpioPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF)
    gpioPin
  }

  def unprovisionPin(pin: GpioPin): Unit = gpioController.unprovisionPin(pin)

  def digitalInputPin(pin: DigitalPin, pinPullResistance: PinPullResistance = PinPullResistance.PULL_UP): GpioPinDigitalInput =
    provision(pin, PinMode.DIGITAL_INPUT) {
      _.provisionDigitalInputPin(pin, pinPullResistance)
    }.asInstanceOf[GpioPinDigitalInput]

  def digitalOutputPin(pin: DigitalPin, defaultState: PinState = PinState.LOW): GpioPinDigitalOutput =
    provision(pin, PinMode.DIGITAL_OUTPUT) {
      _.provisionDigitalOutputPin(pin, defaultState) |> setShutdownOptions
    }.asInstanceOf[GpioPinDigitalOutput]

  def digitalPwmOutputPin(pin: DigitalPwmPin, defaultValue: Int = 0): GpioPinPwmOutput =
    provision(pin, PinMode.PWM_OUTPUT) {
      _.provisionPwmOutputPin(pin, defaultValue)
    }.asInstanceOf[GpioPinPwmOutput]

  def shutdown(): Unit = {
    logger.info("Shutdown all provisioned pins...")
    gpioController.getProvisionedPins.filter(_.getMode == PinMode.PWM_OUTPUT).foreach(_.asInstanceOf[GpioPinPwmOutput].setPwm(0))
    gpioController.unprovisionPin(gpioController.getProvisionedPins.toSeq: _*)
    gpioController.shutdown()
  }

}
