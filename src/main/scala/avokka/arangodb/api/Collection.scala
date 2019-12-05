package avokka.arangodb.api

import avokka.velocypack._
import avokka.velocypack.codecs.VPackRecordCodec
import scodec.Codec

case class Collection
(
  error: Boolean,
  code: Long,
  result: Vector[VPackObject]
)

object Collection {
  implicit val codec: Codec[Collection] = VPackRecordCodec[Collection].codec
}


