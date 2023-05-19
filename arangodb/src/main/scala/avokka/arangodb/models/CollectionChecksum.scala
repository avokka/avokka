package avokka.arangodb
package models

import avokka.velocypack._

final case class CollectionChecksum(
    checksum: String,
    revision: String,
)

object CollectionChecksum {

  implicit val decoder: VPackDecoder[CollectionChecksum] = VPackDecoder.derived

}
