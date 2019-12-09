package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class CollectionRevision
(
  revision: String,
)

object CollectionRevision {
  implicit val codec: Codec[CollectionRevision] = VPackRecord[CollectionRevision].codec
}


