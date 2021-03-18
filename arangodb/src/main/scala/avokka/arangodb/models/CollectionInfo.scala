package avokka.arangodb
package models

import avokka.velocypack._
import types._

/**
  * collection information
  *
  * @param id identifier of the collection
  * @param name name of the collection
  * @param status status of the collection
  * @param `type` type of the collection
  * @param isSystem if true then the collection is a system collection
  * @param globallyUniqueId unique id of the collection
  */
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
