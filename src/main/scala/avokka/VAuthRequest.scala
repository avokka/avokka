package avokka

import avokka.velocypack._
import avokka.velocypack.codecs.VPackHListCodec
import scodec.Encoder
import shapeless.{::, HNil}

case class VAuthRequest
(
  version: Int = 1,
  `type`: MessageType = MessageType.Authentication,
  encryption: String,
  user: String,
  password: String
)

object VAuthRequest {
  implicit val encoder: Encoder[VAuthRequest] = VPackHListCodec.encoderCompact[
    Int ::
    MessageType ::
    String ::
    String ::
    String ::
    HNil
  ].as
}