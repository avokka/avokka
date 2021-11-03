package avokka.arangodb.fs2

sealed trait ConnectionState extends Product with Serializable

object ConnectionState {
  final case object Disconnected extends ConnectionState
  final case object Connecting extends ConnectionState
  final case object Connected extends ConnectionState
  final case object Error extends ConnectionState
}
