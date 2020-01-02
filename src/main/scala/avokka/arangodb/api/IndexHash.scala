package avokka.arangodb
package api

import avokka.velocypack.VPack.VString
import avokka.velocypack._

/**
  * Creates a hash index for the collection collection-name if it
  * does not already exist. The call expects an object containing the index
  * details.
  *
  * In a sparse index all documents will be excluded from the index that do not
  * contain at least one of the specified index attributes (i.e. fields) or that
  * have a value of null in any of the specified index attributes. Such documents
  * will not be indexed, and not be taken into account for uniqueness checks if
  * the unique flag is set.
  *
  * In a non-sparse index, these documents will be indexed (for non-present
  * indexed attributes, a value of null will be used) and will be taken into
  * account for uniqueness checks if the unique flag is set.
  *
  * Note: unique indexes on non-shard keys are not supported in a cluster.
  *
  * @param fields an array of attribute paths
  * @param unique if true, then create a unique index
  * @param sparse if true, then create a sparse index
  * @param deduplicate if false, the deduplication of array values is turned off
  */
case class IndexHash(
    fields: List[String],
    unique: Boolean = false,
    sparse: Boolean = false,
    deduplicate: Boolean = false,
)

object IndexHash { self =>

  implicit val encoder: VPackEncoder[IndexHash] = VPackRecord[IndexHash].encoder

  implicit val api: Api.Command.Aux[Collection, IndexHash, Index.Response] =
    new Api.Command[Collection, IndexHash] {
      override type Response = Index.Response

      override def header(collection: Collection, command: IndexHash): Request.HeaderTrait =
        Request.Header(
          database = collection.database.name,
          requestType = RequestType.POST,
          request = s"/_api/index",
          parameters = Map("collection" -> collection.name)
        )

      override def encoder: VPackEncoder[IndexHash] = self.encoder.mapObject(_.updated("type", VString("hash")))
    }
}
