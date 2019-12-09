package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class CollectionChecksum
(
  checksum: String,
  revision: String,
)

object CollectionChecksum {
  implicit val codec: Codec[CollectionChecksum] = VPackRecord[CollectionChecksum].codec
}


