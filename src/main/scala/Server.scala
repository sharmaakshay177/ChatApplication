import java.net.{ServerSocket, Socket}
import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}

import scala.collection.mutable

// https://www.codejava.net/java-se/networking/how-to-create-a-chat-console-application-in-java-using-socket
// https://gist.github.com/chatton/14110d2550126b12c0254501dde73616
class UserThread(socket: Socket, server: ChatServer) extends Thread{
  private var writer: PrintWriter = _

  override def run(): Unit ={
    try{
      val input = socket.getInputStream
      val reader = new BufferedReader(new InputStreamReader(input))

      val outputStream = socket.getOutputStream
      writer = new PrintWriter(outputStream, true)

      printUsers()

      val userName = reader.readLine()
      server.addUserName(userName) // adding user name
      server.addUserToMapping(userName, this) // add here to add it to the mappings

      var serverMessage = s"New User Connected -> $userName"
      server.broadcast(serverMessage, this)

      var clientMessage: String = ""
      do{
        clientMessage = reader.readLine()
        // check for private condition will pass string trim,
        // syntax-p-toName-message for private else it will be broad casted
        val messageConditionCheck = clientMessage.split('-').toList
        if(messageConditionCheck.length == 1) { // means this message is broad casted
          serverMessage = s"[$userName]: $clientMessage" // when any one send a message.
          server.broadcast(serverMessage, this)
        }else{
          messageConditionCheck.head match {
            case "p" | "P" =>
              val userThread = server.getUserThreadFromMapping(messageConditionCheck(1))
              server.sendPrivately(messageConditionCheck(2), userThread)
          }
        }

      }while(!clientMessage.equals("bye"))

      server.removeUser(userName, this)
      socket.close()

      serverMessage = s"$userName has Left"
      server.broadcast(serverMessage, this)
    }catch{
      case ex: IOException =>
        println(s"Error in UserThread ${server.getUserNames}")
        ex.printStackTrace()
    }
  }

  def printUsers(): Unit ={
    if(server.hasUsers) writer.println(s"Connected Users: ${server.getUserNames.mkString(",")}")
    else writer.println("No Other User Connected")
  }
  def sendMessage(message: String): Unit = writer.println(message)
}

class ChatServer(port: Int){

  private val userNames = new mutable.HashSet[String]()
  private val userThreads = new mutable.HashSet[UserThread]()
  private val userNameToUserTreadObjectMapping = new mutable.HashMap[String, UserThread]()
  private val UserCommands = List("users") // these are client centric commands will send reply privately to user only

  def execute(): Unit ={
    try{
      val serverSocket = new ServerSocket(port)
      println(s"Chat Server is listing on port - $port")

      while (true){
        val socket = serverSocket.accept()
        println(s"New User Connected")

        val newUser = new UserThread(socket, this)
        // add new User to the mappings
        userThreads.add(newUser)
        newUser.start()
      }
    }
    catch {
      case ex: IOException =>
        println(s"Error in Server:  ${ex.getStackTrace.mkString("Array(", ", ", ")")}")
        ex.printStackTrace()
    }
  }

  def broadcast(message: String, excludeUser: UserThread): Unit ={
    for(user <- userThreads){
      if(user !=  excludeUser){
        user.sendMessage(message)
      }
    }
  }

  def sendPrivately(message: String, sendToUserName: UserThread): Unit ={
    for(user <- userThreads){
      if(user == sendToUserName) user.sendMessage(message)
    }
  }
  def checkIfUserAlreadyExist(username: String): Boolean = ???
  def getUserCommandResponse: Unit = ???
  def addUserName(userName: String): Unit = userNames.add(userName)
  def addUserToMapping(userName: String, userThread: UserThread): Unit = userNameToUserTreadObjectMapping.addOne(userName, userThread)
  def getUserThreadFromMapping(userName: String): UserThread ={
    val userThread = userNameToUserTreadObjectMapping.get(userName) match {
      case Some(value) => value
    }
    userThread
  }
  def removeUser(userName: String, user: UserThread): Unit ={
    userNameToUserTreadObjectMapping.remove(userName) // removing it from mapping
    val removed = userNames.remove(userName)
    if(removed) {
      userThreads.remove(user)
      println(s"The User $userName Left")
    }
  }
  def getUserNames: Set[String] = this.userNames.toSet
  def hasUsers: Boolean = this.userNames.nonEmpty
}

object ChatServer{
  def apply(port: Int): ChatServer = new ChatServer(port)
}

object Server extends App {
  val port = 8989 // readInt()
  val chatServer = ChatServer(port)
  chatServer.execute()
}
