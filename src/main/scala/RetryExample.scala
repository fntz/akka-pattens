
import akka.actor.SupervisorStrategy.Restart
import akka.actor._

object RetryExample extends CommonSystem {

  case class Result(x: Int)

  class MyActor extends Actor {
    def receive = {
      case x: Int =>
        val r = 10 / x
        sender ! Result(r)
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      message.foreach {
        case x: Int =>
          self.tell(x + 1, sender)
        case _ =>
          sender ! Result(-1)
      }
    }
    // or something like
    /*
      override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
       sender ! InternalServerError("oops")
    }
     */

  }

  class SuperVisorActor extends Actor {

    override val supervisorStrategy = OneForOneStrategy() {
      case _: Throwable =>
        Restart
    }

    var o = context.system.deadLetters

    def receive = {
      case x: Int =>
        val m = context.actorOf(Props[MyActor])
        m ! x

      case Result(r) =>
        println(s"result: $r")

    }
  }

  val c = system.actorOf(Props[SuperVisorActor])
  c ! 1
  c ! 0


}