package avokka.arangodb

import avokka.velocypack._
import scodec.Encoder
import shapeless.{::, HNil}

case class RequestAuthentication
(
  version: Int = 1,
  `type`: MessageType = MessageType.Authentication,
  encryption: String,
  user: String,
  password: String
)

object RequestAuthentication {

  implicit val encoder: Encoder[RequestAuthentication] = VPackGeneric.encoderCompact[
    Int ::
    MessageType ::
    String ::
    String ::
    String ::
    HNil
  ].as

}