package avokka

import avokka.velocypack.{VPackValue, VelocypackArrayCodec}
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
  val codec: scodec.Codec[VAuthRequest] = VelocypackArrayCodec.codecCompact(
    VPackValue.vpInt ::
    VPackValue.vpInt ::
    VPackValue.vpString ::
    VPackValue.vpString ::
    VPackValue.vpString ::
    HNil
  ).as
}