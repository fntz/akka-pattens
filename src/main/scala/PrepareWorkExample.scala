
import akka.actor._

object PrepareWorkExample extends CommonSystem {

  case object Start
  case class Message1(x: Int)
  case class Result1(x: Int)
  case class Message2(x: Int)
  case class Result2(x: Int)

  case class Result(x: Int)

  class SomeActor extends Actor {
    def receive = {
      case Message1(x) =>
        sender ! Result1(x + 1)
      case Message2(x) =>
        sender ! Result2(x * 10)
    }
  }

  class PrepareActor(f: (Result1, Result2) => Props) extends Actor {

    val someActorRef = context.actorOf(Props[SomeActor])

    var originalMessage: Any = null
    var originalSender: ActorRef = null

    var result1: Result1 = null

    def receive = {
      case r: Result1 =>
        result1 = r
        someActorRef ! Message2(2)

      case result2: Result2 =>
        context.actorOf(f(result1, result2))
          .tell(originalMessage, originalSender)

      case m: Any =>
        originalMessage = m
        originalSender = sender

        someActorRef ! Message1(1)

    }
  }

  class C(r1: Result1, r2: Result2) extends Actor {
    def receive = {
      case Start =>
        sender ! Result(r1.x + r2.x)
    }
  }

  object C {
    def props(r1: Result1, r2: Result2) = Props(classOf[C], r1, r2)
  }


  class UserLand extends Actor {
    def receive = {
      case Start =>
        val f = (r1: Result1, r2: Result2) => C.props(r1, r2)
        val proxy = context.actorOf(Props(classOf[PrepareActor], f))
        proxy ! Start

      case x =>
        println(s"result: $x")
    }
  }

  val u = system.actorOf(Props[UserLand])
  u ! Start


}