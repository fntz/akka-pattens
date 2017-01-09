
import akka.actor._
import scala.concurrent.duration._

trait CommonSystem extends App {
  val system = ActorSystem("my-system")
  system.scheduler.scheduleOnce(10 seconds){system.terminate()}(system.dispatcher)
}
