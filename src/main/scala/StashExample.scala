
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

object StashExample extends CommonSystem {

  class A extends Actor with Stash {

    import context.become

    val threshold = 3

    def intReceive: Receive = {
      case x: Int =>
        if (x == threshold) {
          unstashAll()
          become(intHandlerReceive)
          // !
          self forward x
        } else {
          println(s"stash: $x")
          stash()
        }
    }

    def intHandlerReceive: Receive = {
      case x: Int =>
        sender ! (x + 1)
    }


    def receive = intReceive
  }

  val a = system.actorOf(Props[A])

  import system.dispatcher
  implicit val timeout = Timeout(3 seconds)

  (1 to 4).foreach { x =>
    (a ? x).foreach { r =>
      println(s"$x + 1 => $r")
    }
  }



  /*
    output:
    stash: 1
    stash: 2
    1 + 1 => 2
    2 + 1 => 3
    // 3 ? need: `self forward x`
    4 + 1 => 5

   */



}

