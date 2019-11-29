package avokka

import avokka.velocypack.VPackValue
import avokka.velocypack.codecs.{VPackArrayCodec, VPackHListCodec, VPackObjectCodec}
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
  val codec: Codec[VRequest] = VPackArrayCodec.hlistCompact(
    velocypack.intCodec ::
    velocypack.intCodec ::
    velocypack.stringCodec ::
    velocypack.intCodec ::
    velocypack.stringCodec ::
    VPackObjectCodec.mapOf(velocypack.stringCodec) ::
    VPackObjectCodec.mapOf(velocypack.stringCodec) ::
    HNil
  ).as
}
