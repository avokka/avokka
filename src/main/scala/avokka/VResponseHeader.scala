package avokka

import avokka.velocypack._
import avokka.velocypack.codecs.VPackHListCodec
import scodec.Codec
import shapeless.{::, HNil}

case class VResponseHeader
(
  version: Int,
  `type`: MessageType,
  responseCode: Int,
  meta: Map[String, String] = Map.empty
)

object VResponseHeader {
  implicit val codec: Codec[VResponseHeader] = VPackHListCodec.codecCompact[
    Int ::
    MessageType ::
    Int ::
    Map[String, String] ::
    HNil
  ].as
}