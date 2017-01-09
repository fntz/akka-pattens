
import akka.actor._

object BecomeUnbecomeExample extends CommonSystem {

  case object A
  case object B
  case object C
  case object D


  class MyActor extends Actor {

    import context._

    def a2b: Receive = {
      case A =>
        println("a2b")
        become(b2c)
    }

    def b2c: Receive = {
      case B =>
        println("b2c")
        become(skip)
    }

    def skip: Receive = {
      case C =>
        println("c2d")
        unbecome()
    }

    def receive = a2b

    override def unhandled(message: Any): Unit = {
      println(s"unhandled: $message")
    }
  }

  class MyActor1 extends Actor {

    import context._

    def a2b: Receive = {
      case A =>
        println("a2b")
        become(b2c, discardOld = false)
    }

    def b2c: Receive = {
      case B =>
        println("b2c")
        become(skip, discardOld = false)
    }

    def skip: Receive = {
      case C =>
        println("c2d")
        unbecome()
    }

    def receive = a2b

    override def unhandled(message: Any): Unit = {
      println(s"unhandled: $message")
    }
  }


  val ref = system.actorOf(Props[MyActor])
  val ref1 = system.actorOf(Props[MyActor1])

  ref ! A // => a2b
  ref ! B // => b2c
  ref ! C // => c2d
  ref ! D // => unhandled: D
  ref ! A // => a2b

  // -----------

  ref1 ! A // => a2b
  ref1 ! B // => b2c
  ref1 ! C // => c2d
  ref1 ! D // => unhandled: D
  ref1 ! A // => unhandled: A
  ref1 ! B // => b2c




}
