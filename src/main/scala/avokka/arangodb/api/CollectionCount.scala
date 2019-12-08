package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class CollectionCount
(
  count: Long,
)

object CollectionCount {
  implicit val codec: Codec[CollectionCount] = VPackRecord[CollectionCount].codec
}


