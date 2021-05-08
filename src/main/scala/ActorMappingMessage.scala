import akka.actor.{Actor, ActorRef, ActorSystem, Props}


case class ActorMessage(fromUserId: String, toUserId: String, message: String, toUserName: String)
case class GetUser(toName: String)

class SendMessage(nameActor: ActorRef) extends Actor{

  var localMessageReference: ActorMessage = null

  def receive: Receive ={
    case (userMap: Map[String, String] ,message: ActorMessage) =>
      localMessageReference = message
      println("Base Actor starting received the message object")
      nameActor ! (userMap, message.toUserId)
    case user: String =>
      println(s"the user name received is $user")
      localMessageReference.copy(toUserName = user)
      context.stop(self)
  }
}

class CheckIdReturnName() extends Actor{
  def receive: Receive ={
    case (userMap: Map[String, String], getUser: String)=>
      println(s"Message received and user id to check For user - $getUser")
      val user = userMap.getOrElse(getUser, "UserNotFound")
      println(s"User that we found is $user")
      sender() ! user
  }
}

class SumActor extends Actor{
  def receive: Receive ={
    case (a:Int, b:Int) =>
      val c = a + b
      c
  }
}




object ActorMappingMessage extends App{

  val UserNameMapping = Map[String, String]("sharmaakshay177" -> "Akshay Sharma",
                                            "ls999669" -> "Lucky")
  val idToFind = "ls999669"

  val ChatSystem = ActorSystem("ChatSystem")
  val getUsernameActor = ChatSystem.actorOf(Props[CheckIdReturnName], "GetUserName")
  val sendMessageActor = ChatSystem.actorOf(Props(new SendMessage(getUsernameActor)), "sendMessageActor")

  println("Starting the actor ecosystem")
  var message = ActorMessage("sharmaakshay177", "ls999669", "Hi this is a message from Akshay to Lucky", "")
  val user = sendMessageActor ! (UserNameMapping, message)
  println(user)

//  val sumActor = ChatSystem.actorOf(Props[SumActor], "Sum")
//  sumActor ! (10, 20)
}
