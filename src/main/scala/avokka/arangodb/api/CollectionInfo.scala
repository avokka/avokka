package avokka.arangodb.api

import avokka.arangodb.{CollectionStatus, CollectionType}
import avokka.velocypack._
import scodec.Codec

case class CollectionInfo
(
  id: String,
  name: String,
  status: CollectionStatus,
  `type`: CollectionType,
  isSystem : Boolean,
  globallyUniqueId : String,
)

object CollectionInfo {
  implicit val codec: Codec[CollectionInfo] = VPackRecord[CollectionInfo].codecWithDefaults
}
