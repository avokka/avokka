package avokka.velocystream

import avokka.velocypack._
import avokka.velocypack.codecs.VPackHListCodec
import scodec.Encoder
import shapeless.{::, HNil}

case class VAuthRequest
(
  version: Int,
  `type`: Int,
  encryption: String,
  user: String,
  password: String
)

object VAuthRequest {
  implicit val encoder: Encoder[VAuthRequest] = VPackHListCodec.encoderCompact[
    Int ::
    Int ::
    String ::
    String ::
    String ::
    HNil
  ].as
}