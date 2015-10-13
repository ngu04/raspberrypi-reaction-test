#### Plain scala
This solution relies on only built in scala features.
```scala
implicit class GPIOPinConversion(gpioPinInput: GpioPinInput) {
def addStateChangeEventListener(eventHandler: GpioPinDigitalStateChangeEvent => Unit): Unit =
  gpioPinInput.addListener(new GpioPinListenerDigital {
    override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
      eventHandler(event)
    }
  })
}
```
I extended the plain vanilla java dsl with some scala features. Now I can use a ***function for event handling*** like this:
```scala
val stopButton = pinController.digitalInputPin(BCM_24("Stop"))
stopButton.addStateChangeEventListener { event =>
  stopButton.removeAllListeners()
  progressIndicatorLed.setPwm(Int.MaxValue)
  logger.debug(s"$counter Reaction test session is interrupted!")
}
```
The test itself is a stream of individual reaction tests which is produced by a ***Stream*** 
```scala
private def reactionTestStream(reactionTestResult: ReactionTestResult = ReactionTestResult()): Stream[ReactionTestResult] = {

  ...
  
   currentTestResult #:: (if (progressIndicatorValueBelowTestEndThreshold()) reactionTestStream(currentTestResult) else Stream.empty )
}
```
The stream produces the next iteration as long as the red led doesn't reach its brightest state. The **last** method simply gives back the aggregated results. Booom..
```scala
reactionTestStream().last
```
There are two competing events which could terminate an iteration:
- The led is going to be ON for a certain time. If it goes OFF the current test has been finished.  
- The user has to push the proper button in time.
The first event will define the outcome of the current iteration. ***Future combinators*** for everybody! 
```scala
Await.result(Future.firstCompletedOf(Seq(
  pulseTestLed(reactionTestType),
  buttonReaction(reactionTestType)
)), reactionLedPulseLength * 2 millis)
```
Since the push event is handled by a listener I used a ***promise*** to complete the future 
```scala
  def waitForUserReaction(reactionTestType: Int): Future[Unit] = {
    val promise = Promise[Unit]

    logger.debug(s"$counter. start listener for ${reactionLeds(reactionTestType)}")
    reactionButtons(reactionTestType).addStateChangeEventListener { event =>
      logger.debug(s"$counter. button pushed")
      reactionLeds(reactionTestType).setState(PinState.LOW)
      promise.success((): Unit)
    }

    promise.future
  }
```

## How to run
**pi4j** requires **wiringPi** to be deployed on Raspberry Pi. After it is there you can run the test with the following command:
 ```
 sudo java -jar gpio.jar
 ```
 
This is my scala reaction tester application solution on Raspberry.
