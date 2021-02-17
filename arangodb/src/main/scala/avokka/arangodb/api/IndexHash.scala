package avokka.arangodb
package api

import avokka.velocypack._
import types._

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
final case class IndexHash(
    collection: CollectionName,
    fields: List[String],
    unique: Boolean = false,
    sparse: Boolean = false,
    deduplicate: Boolean = false,
)

object IndexHash { self =>

  implicit val encoder: VPackEncoder[IndexHash] = VPackEncoder.gen

/*  implicit val api: Api.Command.Aux[ArangoDatabase, IndexHash, Index.Response] =
    new Api.Command[ArangoDatabase, IndexHash] {
      override type Response = Index.Response

      override def header(database: ArangoDatabase, command: IndexHash): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.POST,
          request = s"/_api/index",
          parameters = Map("collection" -> command.collection.repr)
        )

      override def encoder: VPackEncoder[IndexHash] =
        self.encoder.mapObject(_.updated("type", "hash"))
    }*/
}
