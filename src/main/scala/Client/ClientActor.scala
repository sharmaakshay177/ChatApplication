package Client

import java.io.{IOException, ObjectInputStream, ObjectOutputStream}
import java.net.{Socket, UnknownHostException}

import Models.{MessageExchange, UserName}
import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props, Timers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.StdIn.readLine

case object StartActors
case object WriteMessages
case object ReadMessages

// https://learning.oreilly.com/library/view/building-applications-with/9781786461483/ch08s06.html
// https://blog.knoldus.com/a-simple-remote-chat-application-using-lift-comet-and-akka-actor-in-scala/
// 


class ReaderActor(socket: Socket, mainChatClient: MainChatClient) extends Actor with Timers{
  import context._
  private var objectReader: ObjectInputStream =_

  override def preStart(): Unit ={
    try{
      objectReader = new ObjectInputStream(socket.getInputStream)
    } catch {
      case ex: IOException => println(s"Error getting Input Stream ${ex.getMessage}")
    }
  }

  override def receive: Receive ={
//    case StartActors =>
//      self ! ReadMessages
    case ReadMessages =>
      //timers.startTimerAtFixedRate("","timerProcessingMessage", 10.microsecond)
      val messageObject = objectReader.readObject().asInstanceOf[MessageExchange]
      println(messageObject.messageContent)
      //if(!mainChatClient.getUserName.isEmpty) println(s"[Type Message ${mainChatClient.getUserName}]:")
//      val writeMessageActor = this.context.child("writerActor") match {
//        case Some(actorRef) => actorRef
//      }
      //self ! ReadMessages
  }

}


class WriterActor(socket: Socket, mainChatClient: MainChatClient, readerActorRef: ActorRef) extends Actor{
  private var objectWriter: ObjectOutputStream =_

  override def preStart(): Unit ={
    try{
      objectWriter = new ObjectOutputStream(socket.getOutputStream)
    } catch {
      case ex: IOException => println(s"Error getting output Stream ${ex.getMessage}")
    }
  }

  override def receive: Receive ={
    case StartActors =>
      val userName = readLine("Enter Your Name")
      objectWriter.writeObject(UserName(userName))
      mainChatClient.setUserName(userName)
      //self ! WriteMessages

    case WriteMessages =>
      readerActorRef ! ReadMessages
      val text = readLine(s"[${mainChatClient.getUserName}: Type Message]")

      if(text.equals("Bye") || text.equals("bye")) self ! "Bye"
      else{
        val textSplit = text.split('-')
        textSplit.length match {
          case 1 =>
            objectWriter.writeObject(MessageExchange(messageContent = textSplit.head))
            self ! WriteMessages
          case 3 =>
            objectWriter.writeObject(MessageExchange(isMessagePrivate = true, textSplit(1), textSplit(2)))
            self ! WriteMessages
        }
      }

    case "Bye" =>
      // sending notification message to server
      objectWriter.writeObject(MessageExchange(messageContent = "bye"))
      // user getting out of the chat
      socket.close()
      readerActorRef ! PoisonPill
      self ! PoisonPill
      context.system.terminate()
  }

}

class MainChatClient(hostname: String, port: Int){
  private var localUser: String =_

  def execute(): Unit ={
    try{
      val socket = new Socket(hostname, port)
      println("Connected to the Chat Server")

      val clientSystem = ActorSystem("ClientSystem")
      val readActor = clientSystem.actorOf(Props(new ReaderActor(socket, this)), "readActor")
      val writeActor = clientSystem.actorOf(Props(new WriterActor(socket, this, readActor)), "writerActor")
      // actor starting
      // readActor ! StartActors
      writeActor ! StartActors
      // actor working
      implicit val executionContext: ExecutionContext = clientSystem.dispatcher
      clientSystem.scheduler.scheduleAtFixedRate(10.millis, 50.millis, readActor, ReadMessages)
      writeActor ! WriteMessages

    } catch {
      case ex: UnknownHostException => println(s"Server not Found ${ex.getMessage}")
      case ex: IOException => println(s"Server not Found ${ex.getMessage}")
    }
  }

  def setUserName(username: String): Unit = this.localUser = username
  def getUserName: String = this.localUser
}

object MainChatClient{
  def apply(hostname: String, port: Int): MainChatClient = new MainChatClient(hostname, port)
}

object ClientActor  extends App{
  val hostname = "localhost"
  val port = 8989

  val client: MainChatClient = MainChatClient(hostname, port)
  client.execute()
}
