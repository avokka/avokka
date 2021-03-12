package avokka.arangodb

import api._
import avokka.velocypack._
import cats.Functor
import protocol._
import types._

trait ArangoCollection[F[_]] {

  /**
    * @return collection name
    */
  def name: CollectionName

  /**
    * existing document api
    * @param key document key
    * @return document api
    */
  def document(key: DocumentKey): ArangoDocument[F]

  /**
    * multi documents api
    * @return documents api
    */
  def documents: ArangoDocuments[F]

  /**
    * create the collection
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

  def count(): F[ArangoResponse[CollectionCount]]

  def info(): F[ArangoResponse[CollectionInfo]]

  def revision(): F[ArangoResponse[CollectionRevision]]

  def properties(): F[ArangoResponse[CollectionProperties]]

  def unload(): F[ArangoResponse[CollectionInfo]]

  def truncate(): F[ArangoResponse[CollectionInfo]]

  def drop(isSystem: Boolean = false): F[ArangoResponse[DeleteResult]]

  def rename(newName: CollectionName): F[ArangoResponse[CollectionInfo]]

  /**
    * Create a document
    * @param document document value
    * @param waitForSync Wait until document has been synced to disk.   (optional)
    * @param returnNew Additionally return the complete new document under the attribute *new* in the result.   (optional)
    * @param returnOld Additionally return the complete old document under the attribute *old* in the result. Only available if the overwrite option is used.   (optional)
    * @param silent If set to *true*, an empty object will be returned as response. No meta-data  will be returned for the created document. This option can be used to save some network traffic.   (optional)
    * @param overwrite If set to *true*, the insert becomes a replace-insert. If a document with the same *_key* already exists the new document is not rejected with unique constraint violated but will replace the old document.   (optional)
    * @tparam T document type
    * @todo refactor to documents api
    */
  def insert[T: VPackEncoder: VPackDecoder](
      document: T,
      waitForSync: Boolean = false,
      returnNew: Boolean = false,
      returnOld: Boolean = false,
      silent: Boolean = false,
      overwrite: Boolean = false,
  ): F[ArangoResponse[Document[T]]]

  /**
    * Query all documents in collection
    * @return query
    */
  def all: ArangoQuery[F, VObject]

  def indexes(): F[ArangoResponse[IndexList]]
  def index(id: String): ArangoIndex[F]

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

object ArangoCollection {

  def apply[F[_]: ArangoClient: Functor](database: DatabaseName, _name: CollectionName): ArangoCollection[F] =
    new ArangoCollection[F] {
      override def name: CollectionName = _name

      private val api: String = "/_api/collection/" + name.repr

      override def document(key: DocumentKey): ArangoDocument[F] = ArangoDocument(database, DocumentHandle(name, key))

      override def documents: ArangoDocuments[F] = ArangoDocuments(database, _name)

      override def index(id: String): ArangoIndex[F] = ArangoIndex(database, id)

      override def checksum(withRevisions: Boolean, withData: Boolean): F[ArangoResponse[CollectionChecksum]] =
          GET(
            database,
            api + "/checksum",
            Map(
              "withRevisions" -> withRevisions.toString,
              "withData" -> withData.toString,
            )
          ).execute

      override def count(): F[ArangoResponse[CollectionCount]] =
        GET(database, api + "/count").execute

      override def info(): F[ArangoResponse[CollectionInfo]] =
        GET(database, api).execute

      override def revision(): F[ArangoResponse[CollectionRevision]] =
        GET(database, api + "/revision").execute

      override def properties(): F[ArangoResponse[CollectionProperties]] =
        GET(database, api + "/properties").execute

      override def truncate(): F[ArangoResponse[CollectionInfo]] =
        PUT(database, api + "/truncate").execute

      override def unload(): F[ArangoResponse[CollectionInfo]] =
        PUT(database, api + "/unload").execute

      override def drop(isSystem: Boolean): F[ArangoResponse[DeleteResult]] =
        DELETE(database, api, Map("isSystem" -> isSystem.toString)).execute

      override def rename(newName: CollectionName): F[ArangoResponse[CollectionInfo]] =
        PUT(database, api + "/rename").body(VObject("name" -> newName.toVPack)).execute

      override def create(setup: CollectionCreate => CollectionCreate): F[ArangoResponse[CollectionInfo]] = {
        val options = setup(CollectionCreate(name))
        POST(database, "/_api/collection", options.parameters).body(options).execute
      }

      override def insert[T: VPackEncoder: VPackDecoder](
          document: T,
          waitForSync: Boolean,
          returnNew: Boolean,
          returnOld: Boolean,
          silent: Boolean,
          overwrite: Boolean
      ): F[ArangoResponse[Document[T]]] =
        ArangoClient[F].execute(
          POST(
            database,
            "/_api/document/" + name.repr,
            Map(
              "waitForSync" -> waitForSync.toString,
              "returnNew" -> returnNew.toString,
              "returnOld" -> returnOld.toString,
              "silent" -> silent.toString,
              "overwrite" -> overwrite.toString,
            )
          ).body(document)
        )(
          implicitly[VPackEncoder[T]].mapObject(_.filter(Document.filterEmptyInternalAttributes)),
          implicitly
        )

      override def indexes(): F[ArangoResponse[IndexList]] =
        GET(database, "/_api/index", Map("collection" -> name.repr)).execute

      override def createIndexHash(fields: List[String],
                                   unique: Boolean,
                                   sparse: Boolean,
                                   deduplicate: Boolean): F[ArangoResponse[Index]] =
        POST(
          database,
          "/_api/index",
          Map("collection" -> name.repr)
        ).body(
          VObject(
            "type" -> "hash".toVPack,
            "fields" -> fields.toVPack,
            "unique" -> unique.toVPack,
            "sparse" -> sparse.toVPack,
            "deduplicate" -> deduplicate.toVPack
          )
        ).execute


      override def all: ArangoQuery[F, VObject] =
        ArangoQuery(database,
                    Query[VObject](
                      query = "FOR doc IN @@collection RETURN doc",
                      bindVars = VObject("@collection" -> name.toVPack)
                    ))

    }

}
