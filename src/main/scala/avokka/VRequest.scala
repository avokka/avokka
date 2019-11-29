package avokka

import avokka.velocypack.codecs.VPackHListCodec
import scodec.Codec
import shapeless.{::, HNil}
import avokka.velocypack._

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
  val codec: Codec[VRequest] = VPackHListCodec.codecCompact[
    Int ::
    Int ::
    String ::
    Int ::
    String ::
    Map[String, String] ::
    Map[String, String] ::
    HNil
  ].as
}
