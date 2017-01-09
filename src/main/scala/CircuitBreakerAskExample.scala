
import akka.actor._
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._

object CircuitBreakerAskExample extends CommonSystem {

  case object Start

  case class Result(x: Int)

  class ExternalService extends Actor {
    def receive = {
      case x: Int =>
        if (x == 0) {
          sender ! akka.actor.Status.Failure(new RuntimeException("boom!"))
        } else {
          sender ! Result(x + 1)
        }
    }
  }


  class MyActor(externalService: ActorRef) extends Actor {

    import context.dispatcher

    implicit val timeout = Timeout(2 seconds)

    val breaker = CircuitBreaker(
      scheduler = context.system.scheduler,
      maxFailures = 1,
      callTimeout = 1 second,
      resetTimeout = 3 seconds
    )
      .onOpen(whenOpen)
      .onHalfOpen(whenHalfOpen)
      .onClose(whenClose)

    def receive = {
      case x: Int =>
        breaker.withCircuitBreaker(externalService ? x) pipeTo sender
    }

    def whenOpen() = println("open")
    def whenClose() = println("close")
    def whenHalfOpen() = println("half open")


  }
  // for ask
  class UserLandAskActor extends Actor {

    val externalService = context.actorOf(Props[ExternalService], "external-service")
    val myActor = context.actorOf(Props(classOf[MyActor], externalService))

    def receive = {
      case Start =>
        myActor ! 0
        myActor ! 0
        Thread.sleep(4000) // timeout for reset
        myActor ! 4

      case x =>
        println(s"---> $x")

    }
  }

  val userLandActor = system.actorOf(Props[UserLandAskActor])
  userLandActor ! Start

  // ---> Failure(java.lang.RuntimeException: boom!)
  // open CB
  // ---> Failure(akka.pattern.CircuitBreakerOpenException: Circuit Breaker is open; calls are failing fast)
  // wait 3 seconds
  // half-open
  // close
  // Result(5)


}
