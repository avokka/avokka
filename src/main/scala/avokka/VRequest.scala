package avokka

import avokka.velocypack.VPackValue
import avokka.velocypack.codecs.{VPackHListCodec, VPackObjectCodec}
import scodec.Codec
import shapeless.HNil

case class VRequest
(
  version: Int,
  `type`: Int,
  database: String,
  requestType: Int,
  request: String,
  parameters: Map[String, String] = Map.empty,
  meta: Map[String, String] = Map.empty,
)

object VRequest {
  val codec: Codec[VRequest] = VPackHListCodec.codecCompact(
    VPackValue.vpInt ::
    VPackValue.vpInt ::
    VPackValue.vpString ::
    VPackValue.vpInt ::
    VPackValue.vpString ::
    VPackObjectCodec.mapOf(VPackValue.vpString) ::
    VPackObjectCodec.mapOf(VPackValue.vpString) ::
    HNil
  ).as
}
