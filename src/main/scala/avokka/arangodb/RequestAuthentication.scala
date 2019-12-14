package avokka.arangodb

import avokka.velocypack._
import scodec.Encoder

case class RequestAuthentication
(
  version: Int = 1,
  `type`: MessageType = MessageType.Authentication,
  encryption: String,
  user: String,
  password: String
)

object RequestAuthentication {
  implicit val encoder: VPackEncoder[RequestAuthentication] = VPackGeneric[RequestAuthentication].encoder //codec(true)
  implicit val decoder: VPackDecoder[RequestAuthentication] = VPackGeneric[RequestAuthentication].decoder
  implicit val serializer: Encoder[RequestAuthentication] = encoder.serializer
}