


object testing extends App{
  val messagePrivate = "p-lucky-my message type here"
  val broadcast = "hi this message is for all"

  val splitMessagePrivate = messagePrivate.split('-').toList
  val broadcastMessage = broadcast.split('-').toList

  println(splitMessagePrivate)
  println(broadcastMessage)
  println(broadcastMessage.length)
}
