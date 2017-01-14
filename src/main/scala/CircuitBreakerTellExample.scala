
import akka.actor._
import akka.pattern._

import scala.concurrent.duration._
import scala.language.postfixOps

object CircuitBreakerTellExample extends CommonSystem {

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

  // oops mutable state
  class CircuitBreakerActor(breaker: CircuitBreaker, externalService: ActorRef) extends Actor {

    var originalSender: ActorRef = null

    def receive = {
      case x: Int if breaker.isClosed =>
        originalSender = sender
        externalService ! x

      case x: Int if breaker.isHalfOpen =>
        originalSender = sender
        externalService ! x

      case _: Int =>
        sender ! Status.Failure(new RuntimeException("breaker fail fast"))

      case r: Result =>
        originalSender ! r
        breaker.succeed()

      case t =>
        println(s"===> breaker fail: $t")
        breaker.fail()
    }
  }

  class MyCircuitBreakerTellActor(ref: ActorRef) extends Actor {

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
      case Start =>
        val c1 = create
        val c2 = create
        val c3 = create

        c1 ! 0
        Thread.sleep(4000)
        c2 ! 3

        c3 ! 4

      case x =>
        println(s"given: $x")
    }

    def create =
      context.actorOf(Props(classOf[CircuitBreakerActor], breaker, ref))

    def whenOpen() = println("open")
    def whenClose() = println("close")
    def whenHalfOpen() = println("half open")
  }

  // breaker fail: Failure(java.lang.RuntimeException: boom!)
  // open
  // half-open
  // wait
  // given: Result(4)
  // given: Result(5)

  class UserLandTellActor extends Actor {

    val externalServiceRef =
      context.actorOf(Props(classOf[ExternalService]))
    val cbRef =
      context.actorOf(Props(classOf[MyCircuitBreakerTellActor], externalServiceRef))


    def receive = {
      case Start =>
        cbRef ! Start
    }
  }

  val userLandTellActor = system.actorOf(Props[UserLandTellActor])
  userLandTellActor ! Start

}
