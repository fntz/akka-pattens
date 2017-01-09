
import akka.actor._
import akka.pattern.{pipe, ask}
import akka.util.Timeout

import scala.concurrent.Future

import scala.concurrent.duration._

object LongInitExample extends CommonSystem {

  implicit val timeout = Timeout(4 seconds)
  import system.dispatcher

  // long init

  class MyClass {
    Thread.sleep(1000)
    println("class loaded")

    def calc(x: Int) = x + 1

  }

  class LongInitialize extends Actor with Stash {

    import context.dispatcher
    import context.become

    var deps: MyClass = null

    override def preStart(): Unit = {
      Future { new MyClass } pipeTo self
    }

    def uninitialized: Receive = {
      case md: MyClass =>
        deps = md
        self ! LongInitialize.Done

      case LongInitialize.Done =>
        println("done")

        Future{1}.flatMap { x =>
          Future{x + 1}.map { y => y + 1 }
        }

        unstashAll()
        become(initialized)

      case _ => stash()
    }

    def initialized: Receive = {
      case x: Int =>
        sender ! deps.calc(x + 1)
    }

    override def receive = uninitialized

  }
  object LongInitialize {
    case object Done
  }

  val liRef = system.actorOf(Props[LongInitialize])

  (1 to 10).foreach { x =>
    (liRef ? x).foreach { r =>
      println(s"$x + 1 = $r")
    }
  }

}
