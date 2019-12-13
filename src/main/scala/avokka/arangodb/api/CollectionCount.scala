package avokka.arangodb.api

import avokka.velocypack._

case class CollectionCount
(
  count: Long,
)

object CollectionCount {
  implicit val decoder: VPackDecoder[CollectionCount] = VPackRecord[CollectionCount].decoder
}
