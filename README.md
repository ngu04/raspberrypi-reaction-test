## Raspberry Pi reaction test [![Codacy Badge](https://api.codacy.com/project/badge/grade/2bdf2ea380b24871b9f52d878005a87b)](https://www.codacy.com/app/krisztian-lachata/raspberrypi-reaction-test)
I am always amazed when I see nicely crafted software components. I admire developers who take the time and try to deliver something functional meanwhile it is also fun to read. 
I don't claim that I always fulfil my own criteria but at least I am striving.
What I like even more is when a software component has its hardware counterpart and they are working together to materialize the functionality in some physical effects. Based on this motive eventually I persuaded myself to buy a Raspberry Pi Model 2 with some extensions to be able build my own sandbox.
The main goal of this project is to play around with Raspberry's GPIO capability and see how it goes with different scala frameworks.

### Functionality
The test application itself is effectively a simple reaction measure system. 
- Start the test with the start button
- After it was kicked off it will randomly blink red or green led. You have to push red or green buttons respectively within the configured time range. 
- The progress indicator PWM red led starts from OFF state. Based on your reaction its pulse width will be increased till the point where it reaches its maximum. 
- When the red led reaches its maximum pulse width (brightest) the test is over. Calculated average reaction and number of tries will be printed.
- Better reaction time results longer test period -> more tries -> higher score

That is it!
  
### Tool set
- [RaspberryPi](https://www.raspberrypi.org/products/raspberry-pi-2-model-b/) is the meat of the application
- [Fritzing](http://fritzing.org/home/) for circuit design
- [wiringPi](http://wiringpi.com/) for low level GPIO manipulation
- [pi4j](http://pi4j.com/) java library to interact with the pi
- [pi4j-client](http://github.com/lachata/pi4j-client) to be able to run pi4j code remotely on a desktop
- [RxScala](https://github.com/ReactiveX/RxScala) to try reactive streams
- [Akka](http://doc.akka.io/docs/akka/2.4.0/scala.html?_ga=1.247924037.378696074.1444496540) to have actor support
- [Akka Streams](http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0/scala.html86) to have actor and streams support to wire up the test

### Project structure
In order to be able to try different approaches there are multiple modules in the project:
- [common](common) - it contains the reusable parts
- [gpio](gpio) - it contains the base implementation. Future combinators, promise, stream
- **rx** - it ***will*** contain the RxScala implementation
- **actor** - it contains a slightly modified version of this game using actors. This is being used at Scala Exchange London 2015 at Sky stand. Currently it is sitting at the scala_exchange_2015 branch
- **stream** - it ***will*** contain the akka stream solution
 
### Circuit layout
Items:
- 3 x 330Ohm resistances
- 1 x red led (progress indicator led)
- 1 x RGB led (for red and green lights)
- 4 x buttons
- Breadboard + wires

GPIO usage
- Start button -> BCM_25 (input, PinPullResistance.PULL_UP)
- Stop button -> BCM_24 (input, PinPullResistance.PULL_UP)
- Red led (RGB bulb) -> BCM_19 (output)
- Green led (RGB bulb) -> BCM_13 (output)
- Red button -> BCM_21 (input, PinPullResistance.PULL_UP)
- Green button -> BCM_23 (input, PinPullResistance.PULL_UP)
- Progress indicator -> BCM_12 (PWM output)

![Alt text](docs/reaction_bb.jpg?raw=true "Breadboard")
![Alt text](docs/real.jpg?raw=true "Real")

### Implementation details
The plan is to incrementally add new frameworks and see how I could implement the same functionality with different approaches
- [Plain scala](gpio) :heavy_check_mark:
- RxScala :x:
- [Actors](https://github.com/lachatak/raspberrypi-reaction-test/blob/scala_exchange_2015) :heavy_check_mark:
- Akka Stream :x:
