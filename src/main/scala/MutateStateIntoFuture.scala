
import akka.actor._

import scala.concurrent.Future

object MutateStateIntoFuture extends CommonSystem {

  // anti pattern
  class A extends Actor {

    import context.dispatcher
    var state: Int = 0

    def receive = {
      case _ =>
        state = state + 1
        println(s"actor thread: ${Thread.currentThread().getId}, state=$state")
        Future {
          Thread.sleep(1000)
        }.foreach { _ =>
          state = state + 1
          println(s"future thread: ${Thread.currentThread().getId}, state=$state")
        }
    }
  }

  // the same for ask pattern !

  val a = system.actorOf(Props[A])
  (0 to 25).foreach { x =>
    a ! x
    Thread.sleep(500)
  }
  /*
    sometimes:

    actor thread: 617, state=1
    actor thread: 617, state=2
    actor thread: 617, state=3   !!! oops
    future thread: 616, state=3  !!! oops
    actor thread: 616, state=5
    future thread: 621, state=6
    future thread: 619, state=7
    future thread: 617, state=

   */

}
