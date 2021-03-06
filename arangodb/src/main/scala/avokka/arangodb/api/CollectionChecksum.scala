package avokka.arangodb
package api

import avokka.velocypack._

final case class CollectionChecksum(
    checksum: String,
    revision: String,
)

object CollectionChecksum {

  implicit val decoder: VPackDecoder[CollectionChecksum] = VPackDecoder.gen

}
