package avokka

import avokka.velocypack.codecs.VPackHListCodec
import scodec.Codec
import shapeless.{::, HNil}
import avokka.velocypack._

case class VResponseHeader
(
  version: Int,
  `type`: Int,
  responseCode: Int,
  meta: Map[String, String] = Map.empty
)

object VResponseHeader {
  val codec: Codec[VResponseHeader] = VPackHListCodec.codecCompact[
    Int ::
    Int ::
    Int ::
    Map[String, String] ::
    HNil
  ].as
}