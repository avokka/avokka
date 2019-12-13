package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class CollectionRevision
(
  revision: String,
)

object CollectionRevision {
  implicit val decoder: VPackDecoder[CollectionRevision] = VPackRecord[CollectionRevision].decoderWithDefaults
}


