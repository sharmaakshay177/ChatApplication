import akka.actor.ActorRef


object ChatRoom {
  sealed trait RoomCommand
  //final case class GetSession(screenName: String, replyTo: ActorRef[SessionEvent]) extends RoomCommand

  sealed trait SessionEvent
  //final case class SessionGranted(handle: ActorRef[PostMessage]) extends SessionEvent
  final case class SessionDenied(reason: String) extends SessionEvent
  final case class MessagePosted(screenName: String, message: String) extends SessionEvent

  trait SessionCommand
  final case class PostMessage(message: String) extends SessionCommand
  private final case class NotifyClient(message: MessagePosted) extends SessionCommand
}
