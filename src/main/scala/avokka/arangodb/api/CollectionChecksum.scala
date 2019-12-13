package avokka.arangodb.api

import avokka.velocypack._

case class CollectionChecksum
(
  checksum: String,
  revision: String,
)

object CollectionChecksum {
  implicit val decoder: VPackDecoder[CollectionChecksum] = VPackRecord[CollectionChecksum].decoder
}
