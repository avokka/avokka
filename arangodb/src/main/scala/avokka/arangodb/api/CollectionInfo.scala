package avokka.arangodb
package api

import avokka.velocypack._
import types._

final case class CollectionInfo(
    id: String,
    name: CollectionName,
    status: CollectionStatus,
    `type`: CollectionType,
    isSystem: Boolean,
    globallyUniqueId: String,
)

object CollectionInfo {

  implicit val decoder: VPackDecoder[CollectionInfo] = VPackDecoder.gen

}
