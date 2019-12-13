package avokka.arangodb.api

import avokka.velocypack._

case class CollectionDrop
(
  id: String
)

object CollectionDrop {
  implicit val decoder: VPackDecoder[CollectionDrop] = VPackRecord[CollectionDrop].decoder
}


