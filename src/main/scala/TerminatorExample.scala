
import akka.actor._

import scala.concurrent.duration._

object TerminatorExample extends CommonSystem {

  // how to handle actor per request without memory leak?
  // terminator

  // first approach use scheduler
  class MyActor extends Actor {

    import context.dispatcher

    context.system.scheduler.scheduleOnce(1 second) {
      self ! PoisonPill
    }

    def receive = {
      case x: Int =>
        println(s"----> $x")
    }
  }

  // or Terminator for actors lifetime management
  class Terminator extends Actor {

    import context.dispatcher

    def receive = {
      case Terminator.KillMeAfter(ref, time) =>
        println(s"stop $ref after $time")
        system.scheduler.scheduleOnce(time, ref, PoisonPill)
    }
  }
  object Terminator {
    case class KillMeAfter(me: ActorRef, time: FiniteDuration)
  }

  trait TerminateMe extends Actor {
    val time: FiniteDuration = 3 seconds
    override def preStart(): Unit = {
      system.eventStream.publish(Terminator.KillMeAfter(self, time))
    }
  }

  class A extends TerminateMe {
    def receive = {
      case x: Int =>
        println(s"---> $x")
    }

    override def postStop(): Unit = {
      println("done")
    }

  }

  val myActor = system.actorOf(Props[MyActor])
  myActor ! 1
  Thread.sleep(3000)
  myActor ! 3 // => deadletters

  //

  val t = system.actorOf(Props[Terminator])
  system.eventStream.subscribe(t, classOf[Terminator.KillMeAfter])


  val a = system.actorOf(Props(classOf[A]), "a-actor")
  a ! 1
  Thread.sleep(5000)
  a ! 3 // deadletters

}
