package avokka.arangodb.fs2

sealed trait ConnectionState extends Product with Serializable

object ConnectionState {
  case object Disconnected extends ConnectionState
  case object Connecting extends ConnectionState
  case object Connected extends ConnectionState
  case object Error extends ConnectionState
}
