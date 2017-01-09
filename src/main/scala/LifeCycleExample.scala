
import akka.actor.SupervisorStrategy.Restart
import akka.actor._

object LifeCycleExample extends CommonSystem {


  class B extends Actor {

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      println("pre restart")
    }

    override def postRestart(reason: Throwable): Unit = {
      println("post restart")
    }

    override def postStop(): Unit = {
      println("post stop")
    }

    override def preStart(): Unit = {
      println("pre start")
    }


    override def receive = {
      case _ =>
        throw new RuntimeException("boom")
        sender ! 1
    }
  }

  class A extends Actor {

    override val supervisorStrategy = OneForOneStrategy() {
      case x: Throwable =>
        Restart
    }
    val b = context.actorOf(Props[B])

    def receive = {
      case x =>
        b ! x
    }
  }

  val a = system.actorOf(Props[A])
  a ! 1
  // pre start
  // exception
  // pre restart
  // post restart
  // post stop



}
