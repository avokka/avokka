package avokka.arangodb

import avokka.velocypack._
import avokka.velocypack.codecs.VPackHListCodec
import scodec.Encoder
import shapeless.{::, HNil}

case class AuthRequest
(
  version: Int = 1,
  `type`: MessageType = MessageType.Authentication,
  encryption: String,
  user: String,
  password: String
)

object AuthRequest {
  implicit val encoder: Encoder[AuthRequest] = VPackHListCodec.encoderCompact[
    Int ::
    MessageType ::
    String ::
    String ::
    String ::
    HNil
  ].as
}