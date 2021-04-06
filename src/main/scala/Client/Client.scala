package Client

import java.net.{Socket, UnknownHostException}
import java.io.{BufferedReader, IOException, InputStreamReader, PrintStream, PrintWriter}

import scala.io.StdIn._

class ReadThread(socket: Socket, client: ChatClient) extends Thread{
  private var reader: BufferedReader = _

  try{
    val input = socket.getInputStream
    reader = new BufferedReader(new InputStreamReader(input))
  }catch{
    case ex: IOException => println(s"Error getting Input Stream ${ex.getMessage}")
  }

  override def run(): Unit ={
    while (true){
      try{
        //todo: check if socket got closed then handle it.
        val response = reader.readLine()
        println(response) // here we are getting everything from server
        if(client.getUserName != null) println(s"[Type Message ${client.getUserName}]:")
      }catch {
        case ex: IOException =>
          println(s"Error while inside Message Listener ${ex.getMessage}")
          ex.printStackTrace()
      }
    }
  }
}

class WriteThread(socket: Socket, client: ChatClient) extends Thread{
  private var writer: PrintWriter = _

  try{
    val output = socket.getOutputStream
    writer = new PrintWriter(output, true)
  }catch {
    case ex: IOException =>
      println(s"Error getting output Stream ${ex.getMessage}")
      ex.printStackTrace()
  }

  override def run(): Unit ={
    val userName = readLine("\nEnter Your Name: ")
    writer.println(userName)
    client.setUserName(userName)
    var text = ""
    do{
      text = readLine(s"[$userName]: ")
      writer.println(text)
    }while(!text.equals("bye"))

    try{
      socket.close()
    }catch{
      case ex: IOException => println(s"Error writing to the server ${ex.getMessage}")
    }
  }
}

class ChatClient(userName: String, hostname: String, port: Int){

  private var localUser = userName
  private var socket: Socket = _
  def execute(): Unit ={
    try{
      socket = new Socket(hostname, port)
      println("Connected to the Chat Server")

      new ReadThread(socket, this).start()
      new WriteThread(socket, this).start()
    }catch {
      case ex: UnknownHostException => println(s"Server not Found ${ex.getMessage}")
      case ex: IOException => println(s"I/O Error: ${ex.getMessage}")
    }
  }
  def setUserName(username: String): Unit = this.localUser = username
  def getUserName: String = this.localUser
}

object ChatClient{
  def apply(userName: String, hostname: String, port: Int): ChatClient = new ChatClient(userName, hostname, port)
}

object Client extends App{
  val name = ""// readLine()
  val hostname = "localhost" // readLine()
  val port = 8989 // readInt()

  val client = ChatClient(name, hostname, port)
  client.execute()
}
