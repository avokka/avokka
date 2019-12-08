package avokka.arangodb.api

import avokka.arangodb.{CollectionStatus, CollectionType}
import avokka.velocypack._
import scodec.Codec

case class Collection
(
  id: String,
  name: String,
  status: CollectionStatus,
  `type`: CollectionType,
  isSystem : Boolean,
  globallyUniqueId : String,
)

object Collection {
  implicit val codec: Codec[Collection] = VPackRecord[Collection].codecWithDefaults
}
