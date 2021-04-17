package Models


case class MessageExchange(isMessagePrivate: Boolean = false,
                           messageTo: String = "all",
                           messageContent: String)
