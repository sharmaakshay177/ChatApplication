import java.io.{IOException, ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket}

import Models.{MessageExchange, UserName}
import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props, Timers}
import scala.concurrent.duration._
import scala.collection.mutable.{HashMap, HashSet}

case class SendMessageMessage(message: MessageExchange)
case object StartMessageHandling
case object HandleClientMessages
case object ClientExitMessage

class UserActor(socket: Socket, server: MainChatServer) extends Actor with Timers{

  private var objectWriter: ObjectOutputStream =_
  private var objectReader: ObjectInputStream =_
  private var localUser: String =_

  override def preStart(): Unit ={
    try{
      objectWriter = new ObjectOutputStream(socket.getOutputStream)
      objectReader = new ObjectInputStream(socket.getInputStream)
    } catch {
      case ex: IOException => println(s"Error getting Input or Output Stream ${ex.getMessage}")
    }
  }


  override def receive: Receive ={
    case StartMessageHandling =>
      printUsers // to print the users on other users screen
      val username = objectReader.readObject().asInstanceOf[UserName]
      localUser = username.username
      println(s"User : $localUser Logged Into Server")
      // adding that name to server
      server.addUserName(username.username)
      server.addUserToMapping(username.username, self)
      // printing server message for new user connection
      val message = MessageExchange(messageContent = s"New User Connected -> ${username.username}")
      server.broadcast(message, self)
      // sending self message to handle client message objects
      //self ! HandleClientMessages

    case HandleClientMessages =>
      //timers.startTimerAtFixedRate("","timerProcessingMessage", 5.microsecond)
      val clientObjectReceived = objectReader.readObject().asInstanceOf[MessageExchange]
      // will directly check if the passed message is privately send or not
      if (clientObjectReceived.isMessagePrivate){
        if(clientObjectReceived.messageContent.equals("bye")) self ! ClientExitMessage
        else {
          val actorToSend = server.getUserActorFromMapping(clientObjectReceived.messageTo)
          val message = MessageExchange(messageContent = clientObjectReceived.messageContent)
          server.sendPrivately(message, actorToSend)
          self ! HandleClientMessages
        }
      }
      else {
        if(clientObjectReceived.messageContent.equals("bye")) self ! ClientExitMessage
        else {
          val message = MessageExchange(messageContent = s"[$localUser]: ${clientObjectReceived.messageContent}")
          server.broadcast(message, self)
          self ! HandleClientMessages
        }
      }

    case ClientExitMessage =>
      server.removeUser(localUser, self)
      val message = MessageExchange(messageContent = s"$localUser Has Left The Chat")
      server.broadcast(message, self)
      // closing the socket for that
      socket.close()
      // destroying the actor for that user
      self ! PoisonPill

    case SendMessageMessage(message) =>
      sendMessage(message)
      // reiterated message
      self ! HandleClientMessages
  }

  def sendMessage(message: MessageExchange): Unit = objectWriter.writeObject(message)

  def printUsers: Unit = {
    if(server.hasUsers) {
      val message = s"Users Connected : ${server.getUserNames.mkString(",")}"
      objectWriter.writeObject(MessageExchange(messageContent = message))
    }
    else objectWriter.writeObject(MessageExchange(messageContent = "No Other User Connected"))
  }

}

class MainChatServer(port: Int){
  private val userNames = new HashSet[String]()
  private val userActors = new HashSet[ActorRef]()
  private val userNameToActorsMapping = new HashMap[String, ActorRef]()
  private val Commands = List("users")

  def execute(): Unit ={
    try{
      val serverSocket = new ServerSocket(port)
      println(s"Chat Server is listing on port - $port")

      while (true){
        val socket = serverSocket.accept()
        println(s"New User Connected")

        val ChatSystem = ActorSystem("ServerSystem")
        val newUserActor = ChatSystem.actorOf(Props(new UserActor(socket, this)),s"UserActor${userActors.size+1}")
        // add new User to the mappings
        userActors.add(newUserActor)
        newUserActor ! StartMessageHandling
        newUserActor ! HandleClientMessages
      }
    }catch {
      case ex: IOException =>
        println(s"Error in Server: ${ex.getStackTrace.mkString("Array(", ", ", ")")}")
    }
  }

  def broadcast(message: MessageExchange, excludeActor: ActorRef): Unit ={
    for (userActor <- userActors){
      if (userActor != excludeActor) userActor ! SendMessageMessage(message)
    }
  }

  def sendPrivately(message: MessageExchange, sendToActor: ActorRef): Unit ={
    for (userActor <- userActors){
      if (userActor ==  sendToActor) userActor ! SendMessageMessage(message)
    }
  }

  def checkIfUserAlreadyExist(username: String): Boolean = ???

  def getUserCommandResponse: Unit = ???

  def addUserName(username: String): Unit = userNames.add(username)

  def addUserToMapping(username: String, userActor: ActorRef): Unit = userNameToActorsMapping.addOne(username,userActor)

  def getUserActorFromMapping(username: String): ActorRef = userNameToActorsMapping.get(username) match {
    case Some(userActor) => userActor
  }

  def removeUser(username: String, userActor: ActorRef): Unit ={
    userNameToActorsMapping.remove(username)
    val removed = userNames.remove(username)
    if(removed){
      userActors.remove(userActor)
    }
  }

  def getUserNames: Set[String] = this.userNames.toSet

  def hasUsers: Boolean = this.userNames.nonEmpty
}

object MainChatServer{
  def apply(port: Int): MainChatServer = new MainChatServer(port)
}

object ServerActor  extends App{
  val port = 8989
  val chatServer: MainChatServer = MainChatServer(port)
  chatServer.execute()
}
