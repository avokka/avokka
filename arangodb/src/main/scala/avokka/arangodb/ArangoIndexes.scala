package avokka.arangodb

import models._
import avokka.velocypack._
import types._
import protocol._

trait ArangoIndexes[F[_]] {

  /**
    * Returns an object with an attribute indexes containing an array of all index descriptions for the given collection. The same information is also available in the identifiers as an object with the index handles as keys.
    *
    * @return all indexes of a collection
    */
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
  def createHash(
      fields: List[String],
      unique: Boolean = false,
      sparse: Boolean = false,
      deduplicate: Boolean = false,
      name: Option[String] = None,
  ): F[ArangoResponse[Index]]

  /**
    * Creates a skip-list index for the collection collection-name, if it does not already exist. The call expects an object containing the index details.
    *
    * In a sparse index all documents will be excluded from the index that do not contain at least one of the specified index attributes (i.e. fields) or that have a value of null in any of the specified index attributes. Such documents will not be indexed, and not be taken into account for uniqueness checks if the unique flag is set.
    *
    * In a non-sparse index, these documents will be indexed (for non-present indexed attributes, a value of null will be used) and will be taken into account for uniqueness checks if the unique flag is set.
    *
    * Note: unique indexes on non-shard keys are not supported in a cluster.
    *
    * @param fields an array of attribute paths
    * @param unique if true, then create a unique index
    * @param sparse if true, then create a sparse index
    * @param deduplicate if false, the deduplication of array values is turned off
    */
  def createSkipList(
      fields: List[String],
      unique: Boolean = false,
      sparse: Boolean = false,
      deduplicate: Boolean = false,
      name: Option[String] = None,
  ): F[ArangoResponse[Index]]

  /**
    * Creates a persistent index for the collection collection-name, if it does not already exist. The call expects an object containing the index details.
    *
    * In a sparse index all documents will be excluded from the index that do not contain at least one of the specified index attributes (i.e. fields) or that have a value of null in any of the specified index attributes. Such documents will not be indexed, and not be taken into account for uniqueness checks if the unique flag is set.
    *
    * In a non-sparse index, these documents will be indexed (for non-present indexed attributes, a value of null will be used) and will be taken into account for uniqueness checks if the unique flag is set.
    *
    * Note: unique indexes on non-shard keys are not supported in a cluster.
    *
    * @param fields an array of attribute paths
    * @param unique if true, then create a unique index
    * @param sparse if true, then create a sparse index
    */
  def createPersistent(
      fields: List[String],
      unique: Boolean = false,
      sparse: Boolean = false,
      name: Option[String] = None,
  ): F[ArangoResponse[Index]]

  /**
    * Creates a geo-spatial index in the collection collection-name, if it does not already exist. Expects an object containing the index details.
    *
    * Geo indexes are always sparse, meaning that documents that do not contain the index attributes or have non-numeric values in the index attributes will not be indexed.
    *
    * @param fields An array with one or two attribute paths.
    *               If it is an array with one attribute path location, then a geo-spatial index on all documents is created using location as path to the coordinates. The value of the attribute must be an array with at least two double values. The array must contain the latitude (first value) and the longitude (second value). All documents, which do not have the attribute path or with value that are not suitable, are ignored.
    *               If it is an array with two attribute paths latitude and longitude, then a geo-spatial index on all documents is created using latitude and longitude as paths the latitude and the longitude. The value of the attribute latitude and of the attribute longitude must a double. All documents, which do not have the attribute paths or which values are not suitable, are ignored.
    * @param geoJson If a geo-spatial index on a location is constructed and geoJson is true, then the order within the array is longitude followed by latitude. This corresponds to the format described in http://geojson.org/geojson-spec.html#positions
    */
  def createGeo(
      fields: List[String],
      geoJson: Boolean = false,
      name: Option[String] = None,
  ): F[ArangoResponse[Index]]

  /**
    * Creates a fulltext index for the collection collection-name, if it does not already exist. The call expects an object containing the index details.
    *
    * @param fields an array of attribute names. Currently, the array is limited to exactly one attribute
    * @param minLength Minimum character length of words to index. Will default to a server-defined value if unspecified. It is thus recommended to set this value explicitly when creating the index.
    */
  def createFullText(
      fields: List[String],
      minLength: Option[Int] = None,
      name: Option[String] = None,
  ): F[ArangoResponse[Index]]

  /**
    * Creates a TTL index for the collection collection-name if it does not already exist. The call expects an object containing the index details.
    *
    * @param fields an array with exactly one attribute path
    * @param expireAfter The time (in seconds) after a document’s creation after which the documents count as “expired”.
    */
  def createTtl(
      fields: List[String],
      expireAfter: Int,
      name: Option[String] = None,
  ): F[ArangoResponse[Index]]
}

object ArangoIndexes {
  def apply[F[_]: ArangoClient](database: DatabaseName, collection: CollectionName): ArangoIndexes[F] =
    new ArangoIndexes[F] {

      private val path: String = "/_api/index"

      override def list(): F[ArangoResponse[IndexList]] =
        GET(
          database,
          path,
          Map("collection" -> collection.repr)
        ).execute

      override def createHash(
          fields: List[String],
          unique: Boolean,
          sparse: Boolean,
          deduplicate: Boolean,
          name: Option[String],
      ): F[ArangoResponse[Index]] =
        POST(
          database,
          path,
          Map("collection" -> collection.repr)
        ).body(
            VObject(
              "name" -> name.toVPack,
              "type" -> (Index.Type.hash: Index.Type).toVPack,
              "fields" -> fields.toVPack,
              "unique" -> unique.toVPack,
              "sparse" -> sparse.toVPack,
              "deduplicate" -> deduplicate.toVPack,
            )
          )
          .execute

      override def createSkipList(
          fields: List[String],
          unique: Boolean,
          sparse: Boolean,
          deduplicate: Boolean,
          name: Option[String],
      ): F[ArangoResponse[Index]] =
        POST(
          database,
          path,
          Map("collection" -> collection.repr)
        ).body(
            VObject(
              "name" -> name.toVPack,
              "type" -> (Index.Type.skiplist: Index.Type).toVPack,
              "fields" -> fields.toVPack,
              "unique" -> unique.toVPack,
              "sparse" -> sparse.toVPack,
              "deduplicate" -> deduplicate.toVPack
            )
          )
          .execute

      override def createPersistent(
          fields: List[String],
          unique: Boolean,
          sparse: Boolean,
          name: Option[String],
      ): F[ArangoResponse[Index]] =
        POST(
          database,
          path,
          Map("collection" -> collection.repr)
        ).body(
            VObject(
              "name" -> name.toVPack,
              "type" -> (Index.Type.persistent: Index.Type).toVPack,
              "fields" -> fields.toVPack,
              "unique" -> unique.toVPack,
              "sparse" -> sparse.toVPack,
            )
          )
          .execute

      override def createGeo(
          fields: List[String],
          geoJson: Boolean,
          name: Option[String],
      ): F[ArangoResponse[Index]] =
        POST(
          database,
          path,
          Map("collection" -> collection.repr)
        ).body(
            VObject(
              "name" -> name.toVPack,
              "type" -> (Index.Type.geo: Index.Type).toVPack,
              "fields" -> fields.toVPack,
              "geoJson" -> geoJson.toVPack,
            )
          )
          .execute

      override def createFullText(
          fields: List[String],
          minLength: Option[Int],
          name: Option[String],
      ): F[ArangoResponse[Index]] =
        POST(
          database,
          path,
          Map("collection" -> collection.repr)
        ).body(
            VObject(
              "name" -> name.toVPack,
              "type" -> (Index.Type.fulltext: Index.Type).toVPack,
              "fields" -> fields.toVPack,
              "minLength" -> minLength.toVPack,
            )
          )
          .execute

      override def createTtl(
          fields: List[String],
          expireAfter: Int,
          name: Option[String],
      ): F[ArangoResponse[Index]] =
        POST(
          database,
          path,
          Map("collection" -> collection.repr)
        ).body(
            VObject(
              "name" -> name.toVPack,
              "type" -> (Index.Type.ttl: Index.Type).toVPack,
              "fields" -> fields.toVPack,
              "expireAfter" -> expireAfter.toVPack,
            )
          )
          .execute
    }
}
