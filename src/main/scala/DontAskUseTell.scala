
import akka.actor._

import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration._

object DontAskUseTell  extends CommonSystem {

  case class Result(x: Int)

  class MyService extends Actor {
    def receive = {
      case x: Int =>
        sender ! Result(x + 1)
    }
  }

  class AskExample1 extends Actor {

    // broken actor model
    // what about supervisor?
    // timeout hell
    // class cast exception

    import context.dispatcher
    implicit val timeout = Timeout(3 seconds)

    val a = context.actorOf(Props[MyService])

    def receive = {
      case x: Int =>
        (a ? x).mapTo[Result].foreach { x =>
          println(s"result = $x")
        }
    }
  }


  class Asker(a: ActorRef) extends Actor {

    import context.dispatcher
    implicit val timeout = Timeout(3 seconds)

    var originalSender: ActorRef = _

    def receive = {
      case x: Int =>
        originalSender = sender
        (a ? x) pipeTo self
      case r: Result =>
        originalSender ! r
      case t =>
        println(s"oops: $t")
        originalSender ! Result(-1)
    }
  }

  class AskExample2 extends Actor {

    // supervision ok
    // class cast ok
    // timeout - fail
    // broken actor model - fail


    val a = context.actorOf(Props[MyService])
    def receive = {
      case x: Int =>
        val asker = context.actorOf(Props(classOf[Asker], a))
        asker ! x
      case Result(r) =>
        println(s"result (ask2) = $r")
    }
  }


  class TellExample(a: ActorRef) extends Actor {

    var originalSender: ActorRef = _

    def receive = {
      case x: Int =>
        originalSender = sender
        a ! x

      case r: Result =>
        originalSender ! r
      case t =>
        println(s"oops: $t")
        originalSender ! Result(-1)
    }
  }

  class TellSupervisor extends Actor {

    // supervision ok
    // class cast ok
    // timeout - ok
    // broken actor model - ok


    val a = context.actorOf(Props[MyService])
    def receive = {
      case x: Int =>
        val teller = context.actorOf(Props(classOf[TellExample], a))
        teller ! x
      case Result(r) =>
        println(s"result (tell) = $r")
    }
  }



  val ask1 = system.actorOf(Props[AskExample1])
  ask1 ! 1
  val ask2 = system.actorOf(Props[AskExample2])
  ask2 ! 1
  val tell1 = system.actorOf(Props[TellSupervisor])
  tell1 ! 1


}
