package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack._

case class CollectionInfo
(
  id: String,
  name: CollectionName,
  status: CollectionStatus,
  `type`: CollectionType,
  isSystem : Boolean,
  globallyUniqueId : String,
)

object CollectionInfo {
  implicit val decoder: VPackDecoder[CollectionInfo] = VPackRecord[CollectionInfo].decoderWithDefaults
}
