import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable.ListBuffer

case object CreateChild
case object PrintSignal
case object SignalChildren
case object SelfSignal

class ParentActor extends Actor{
  private var childNumber = 0
  //private val allChild = ListBuffer[ActorRef]()

  override def receive: Receive ={
    case CreateChild =>
      context.actorOf(Props[ChildActor],s"Child$childNumber")
      childNumber += 1
    case SignalChildren =>
      context.children.foreach(child => child ! PrintSignal)
  }
}

class ChildActor extends Actor{
  override def receive: Receive ={
    case PrintSignal =>
      println(self)
  }
}

object ActorHierarchy  extends App{
  val system = ActorSystem("Hierarchy")
  val parent = system.actorOf(Props[ParentActor], "Parent1")
  val parent2 = system.actorOf(Props[ParentActor], "Parent2")

  parent ! CreateChild
  parent ! SignalChildren
  parent ! CreateChild
  parent ! CreateChild
  parent ! SignalChildren

  parent2 ! CreateChild
  // get hold of the child by using the url
  val child0 = system.actorSelection("akka://Hierarchy/user/Parent2/Child0")
  child0 ! PrintSignal

  system.terminate()
}
