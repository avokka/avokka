package avokka.arangodb

import models.{CollectionProperties, _}
import avokka.velocypack._
import cats.Functor
import protocol._
import types._

/**
  * ArangoDB collection API
  *
  * @see [[https://www.arangodb.com/docs/stable/http/collection.html]]
  * @tparam F effect
  */
trait ArangoCollection[F[_]] {

  /** collection name */
  def name: CollectionName

  /** documents api */
  def documents: ArangoDocuments[F]

  /** existing document api */
  def document(key: DocumentKey): ArangoDocument[F]

  /** indexes api */
  def indexes: ArangoIndexes[F]

  /** existing index api */
  def index(id: String): ArangoIndex[F]

  /**
    * Create the collection
    *
    * @param setup modify creation options
    * @return collection information
    */
  def create(setup: CollectionCreate => CollectionCreate = identity): F[ArangoResponse[CollectionInfo]]

  /**
    * Will calculate a checksum of the meta-data (keys and optionally revision ids) and
    * optionally the document data in the collection.
    *
    * The checksum can be used to compare if two collections on different ArangoDB
    * instances contain the same contents. The current revision of the collection is
    * returned too so one can make sure the checksums are calculated for the same
    * state of data.
    *
    * By default, the checksum will only be calculated on the _key system attribute
    * of the documents contained in the collection. For edge collections, the system
    * attributes _from and _to will also be included in the calculation.
    *
    * @param withRevisions include document revision ids in the checksum calculation
    * @param withData      include document body data in the checksum calculation
    */
  def checksum(
      withRevisions: Boolean = false,
      withData: Boolean = false,
  ): F[ArangoResponse[CollectionChecksum]]

  /**
    * Return information about collection
    *
    * @return collection information
    */
  def info(): F[ArangoResponse[CollectionInfo]]

  /**
    * Return collection revision id
    *
    * The revision is a server-generated string that clients can use to check whether data in a collection has changed since the last revision check.
    *
    * @return collection revision
    */
  def revision(): F[ArangoResponse[CollectionRevision]]

  /**
    * Read properties of collection
    *
    * @return collection properties
    */
  def properties(): F[ArangoResponse[CollectionProperties]]

  /**
    * Update properties of collection
    * @param waitForSync  If true then creating or changing a document will wait until the data has been synchronized to disk
    * @param schema       Object that specifies the collection level schema for documents. The attribute keys rule, level and message must follow the rules documented in Document Schema Validation
    * @return collection properties
    */
  def update(waitForSync: Option[Boolean] = None, schema: Option[CollectionSchema] = None): F[ArangoResponse[CollectionProperties]]

  /**
    * Load collection
    *
    * @return collection information
    */
  def load(): F[ArangoResponse[CollectionInfo]]

  /**
    * Unload collection
    *
    * @return collection information
    */
  def unload(): F[ArangoResponse[CollectionInfo]]

  /**
    * Truncate collection
    *
    * @param waitForSync If true then the data is synchronized to disk before returning from the truncate operation
    * @param compact  If true then the storage engine is told to start a compaction in order to free up disk space. This can be resource intensive. If the only intention is to start over with an empty collection, specify false.
    * @return collection information
    */
  def truncate(waitForSync: Boolean = false, compact: Boolean = true): F[ArangoResponse[CollectionInfo]]

  /**
    * Drop collection
    *
    * @param isSystem Whether or not the collection to drop is a system collection. This parameter must be set to true in order to drop a system collection.
    * @return identifier of the dropped collection
    */
  def drop(isSystem: Boolean = false): F[ArangoResponse[DeleteResult]]

  /**
    * Rename collection
    * @param newName
    * @return
    */
  def rename(newName: CollectionName): F[ArangoResponse[CollectionInfo]]

  /**
    * Query all documents in collection
    * @return query
    */
  def all: ArangoQuery[F, VObject]
}

object ArangoCollection {

  def apply[F[_]: ArangoClient: Functor](database: DatabaseName, _name: CollectionName): ArangoCollection[F] =
    new ArangoCollection[F] {
      override def name: CollectionName = _name

      private val path: String = API_COLLECTION + "/" + name.repr

      override def document(key: DocumentKey): ArangoDocument[F] = ArangoDocument(database, DocumentHandle(name, key))

      override val documents: ArangoDocuments[F] = ArangoDocuments(database, _name)

      override def index(id: String): ArangoIndex[F] = ArangoIndex(database, id)

      override val indexes: ArangoIndexes[F] = ArangoIndexes(database, _name)

      override def checksum(withRevisions: Boolean, withData: Boolean): F[ArangoResponse[CollectionChecksum]] =
          GET(
            database,
            path + "/checksum",
            Map(
              "withRevisions" -> withRevisions.toString,
              "withData" -> withData.toString,
            )
          ).execute

      override def info(): F[ArangoResponse[CollectionInfo]] =
        GET(database, path).execute

      override def revision(): F[ArangoResponse[CollectionRevision]] =
        GET(database, path + "/revision").execute

      override def properties(): F[ArangoResponse[CollectionProperties]] =
        GET(database, path + "/properties").execute

      override def update(waitForSync: Option[Boolean], schema: Option[CollectionSchema]): F[ArangoResponse[CollectionProperties]] =
        PUT(
          database,
          path + "/properties",
        ).body(VObject(
          "waitForSync" -> waitForSync.toVPack,
          "schema" -> schema.toVPack
        )).execute

      override def truncate(waitForSync: Boolean, compact: Boolean): F[ArangoResponse[CollectionInfo]] =
        PUT(
          database,
          path + "/truncate",
          Map(
            "waitForSync" -> waitForSync.toString,
            "compact" -> compact.toString,
          )
        ).execute

      override def load(): F[ArangoResponse[CollectionInfo]] =
        PUT(database, path + "/load").execute

      override def unload(): F[ArangoResponse[CollectionInfo]] =
        PUT(database, path + "/unload").execute

      override def drop(isSystem: Boolean): F[ArangoResponse[DeleteResult]] =
        DELETE(database, path, Map("isSystem" -> isSystem.toString)).execute

      override def rename(newName: CollectionName): F[ArangoResponse[CollectionInfo]] =
        PUT(database, path + "/rename").body(VObject("name" -> newName.toVPack)).execute

      override def create(setup: CollectionCreate => CollectionCreate): F[ArangoResponse[CollectionInfo]] = {
        val options = setup(CollectionCreate(name))
        POST(database, API_COLLECTION, options.parameters).body(options).execute
      }

      override def all: ArangoQuery[F, VObject] =
        ArangoQuery(database,
                    Query[VObject](
                      query = "FOR doc IN @@collection RETURN doc",
                      bindVars = VObject("@collection" -> name.toVPack)
                    ))

    }

}
