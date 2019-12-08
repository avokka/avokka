package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class Collection
(
  error: Boolean,
  code: Long,
  result: Vector[CollectionResult]
)

object Collection {
  implicit val codec: Codec[Collection] = VPackRecord[Collection].codec
}


