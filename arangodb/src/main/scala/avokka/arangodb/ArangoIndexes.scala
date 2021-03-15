package avokka.arangodb

import api._
import avokka.velocypack._
import types._
import protocol._

trait ArangoIndexes[F[_]] {
  def list(): F[ArangoResponse[IndexList]]

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
  def createIndexHash(
                       fields: List[String],
                       unique: Boolean = false,
                       sparse: Boolean = false,
                       deduplicate: Boolean = false,
                     ): F[ArangoResponse[Index]]
}

object ArangoIndexes {
  def apply[F[_]: ArangoClient](database: DatabaseName, collection: CollectionName): ArangoIndexes[F] = new ArangoIndexes[F] {

    override def list(): F[ArangoResponse[IndexList]] =
      GET(
        database,
        "/_api/index",
        Map("collection" -> collection.repr)
      ).execute

    override def createIndexHash(fields: List[String],
                                 unique: Boolean,
                                 sparse: Boolean,
                                 deduplicate: Boolean): F[ArangoResponse[Index]] =
      POST(
        database,
        "/_api/index",
        Map("collection" -> collection.repr)
      ).body(
        VObject(
          "type" -> "hash".toVPack,
          "fields" -> fields.toVPack,
          "unique" -> unique.toVPack,
          "sparse" -> sparse.toVPack,
          "deduplicate" -> deduplicate.toVPack
        )
      ).execute

  }
}