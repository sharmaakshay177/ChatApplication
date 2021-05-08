
import akka.actor.{Actor, ActorSystem, Props, SupervisorStrategy}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

case class NameResponse(Firstname: String, Lastname: String)
case object AskFullName

class ActorAsk(name: String) extends Actor{

  def receive: Receive ={
    case AskFullName => sender() ! NameResponse(name, "Sharma")
  }

  // will only come to effect if no case message found under receive
  override def unhandled(message: Any): Unit = ???

  // self used to point to himself
  // sender() -> use to point to the sender of the message
  // context: ActorContext -> to get teh contextual info of the system, will provide the
  // context of current message, factor method to create child actors.
  // override def supervisorStrategy: SupervisorStrategy = super.supervisorStrategy
  // it defines the strategy what happen when a failure is detected in an actor.
  // override def preStart(): Unit = super.preStart()
  // it is called when actor is started for the first time, it will be called before any
  // messages will be handled. it can be used to initialize the resources
  def appendSirName(name: String): String = name + "sharma"
}


// work with ask pattern in actor to return info as well
object ActorThirdExample extends App{
  val system = ActorSystem("MySystem")
  val miniActor = system.actorOf(Props(new ActorAsk("Akshay")), "AskActor")

  implicit val timeout: Timeout = Timeout(1.seconds)

  // ! is used to send the message
  // ! is fire and forget method or tell
  // ? used to ask pattern expecting something in return
  // ask method or ? is used to get the future
  val fullNameFuture = miniActor ? AskFullName
  val fullName = Await.result(fullNameFuture, timeout.duration).asInstanceOf[NameResponse]
  println("Full Name from object")
  println(fullName)

  val nameObject: Future[NameResponse] = ask(miniActor, AskFullName).mapTo[NameResponse]
  val name = Await.result(nameObject, timeout.duration)
  println("using foreach to print")
  nameObject.foreach(item => println(s"FirstName: ${item.Firstname} and LastName: ${item.Lastname}"))
  println(s"name extracted $name")
  // extract response on success


  //val name = Await.result(fullName, timeout.duration)
  //fullName.foreach(n => println(s"Full Name is $n"))
  system.terminate()
}
