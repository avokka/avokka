package avokka.arangodb

import avokka.velocypack._
import scodec.{Codec, Encoder}

case class RequestAuthentication
(
  version: Int = 1,
  `type`: MessageType = MessageType.Authentication,
  encryption: String,
  user: String,
  password: String
)

object RequestAuthentication {
  implicit val codec: Codec[RequestAuthentication] = VPackGeneric[RequestAuthentication].codec(true)
}