import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.event.Logging
import language.postfixOps
import scala.concurrent.duration._

case class Ping()
case class Pong()

class Pinger extends Actor{
  var countDown = 100

  override def receive: Receive ={
    case Pong =>
      println(s"${self.path} received pong, count down $countDown")
      if (countDown > 0) {countDown -=1; sender() ! Ping}
      else {sender() ! PoisonPill; self ! PoisonPill}
  }
}

class Ponger(pinger: ActorRef) extends Actor{
  def receive: Receive ={
    case Ping =>
      println(s"${self.path} received ping")
      pinger ! Pong
  }
}

object ActorLearning extends App{
  // creating the system
  val system = ActorSystem("PingPong")
  val pinger = system.actorOf(Props[Pinger](), "Pinger")
  val ponger = system.actorOf(Props(classOf[Ponger], pinger), "Ponger")

  import system.dispatcher
  system.scheduler.scheduleOnce(500 millis){
    ponger ! Ping
  }
}
