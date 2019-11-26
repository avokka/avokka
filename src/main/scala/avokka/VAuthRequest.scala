package avokka

import avokka.velocypack.VPackValue
import avokka.velocypack.codecs.VPackHListCodec
import scodec.Codec
import shapeless.HNil

case class VAuthRequest
(
  version: Int,
  `type`: Int,
  encryption: String,
  user: String,
  password: String
)

object VAuthRequest {
  val codec: Codec[VAuthRequest] = VPackHListCodec.codecCompact(
    VPackValue.vpInt ::
    VPackValue.vpInt ::
    VPackValue.vpString ::
    VPackValue.vpString ::
    VPackValue.vpString ::
    HNil
  ).as
}