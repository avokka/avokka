package avokka.velocystream

import avokka.velocypack._
import avokka.velocypack.codecs.VPackHListCodec
import scodec.Codec
import shapeless.{::, HNil}

case class VRequestHeader
(
  version: Int,
  `type`: Int,
  database: String,
  requestType: Int,
  request: String,
  parameters: Map[String, String] = Map.empty,
  meta: Map[String, String] = Map.empty,
)

object VRequestHeader {
  implicit val codec: Codec[VRequestHeader] = VPackHListCodec.codecCompact[
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
