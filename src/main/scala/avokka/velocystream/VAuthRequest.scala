package avokka.velocystream

import avokka.velocypack._
import avokka.velocypack.codecs.VPackHListCodec
import scodec.Codec
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
  implicit val codec: Codec[VAuthRequest] = VPackHListCodec.codecCompact[
    Int ::
    Int ::
    String ::
    String ::
    String ::
    HNil
  ].as
}