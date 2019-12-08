package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class CollectionResult
(
  id: String,
  name: String,
  status: Int,
  `type`: Int,
  isSystem : Boolean,
  globallyUniqueId : String,
)

object CollectionResult {
  implicit val codec: Codec[CollectionResult] = VPackRecord[CollectionResult].codecWithDefaults
}
