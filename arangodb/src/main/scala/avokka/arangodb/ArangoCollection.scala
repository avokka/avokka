package avokka.arangodb

import api._
import avokka.velocypack._
import protocol._
import types._

trait ArangoCollection[F[_]] {
  def database: ArangoDatabase[F]
  def name: CollectionName

  def document(key: DocumentKey): ArangoDocument[F]

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
  def drop(isSystem: Boolean = false): F[ArangoResponse[CollectionDrop]]

  /**
    * Create a document
    * @param document document value
    * @param waitForSync Wait until document has been synced to disk.   (optional)
    * @param returnNew Additionally return the complete new document under the attribute *new* in the result.   (optional)
    * @param returnOld Additionally return the complete old document under the attribute *old* in the result. Only available if the overwrite option is used.   (optional)
    * @param silent If set to *true*, an empty object will be returned as response. No meta-data  will be returned for the created document. This option can be used to save some network traffic.   (optional)
    * @param overwrite If set to *true*, the insert becomes a replace-insert. If a document with the same *_key* already exists the new document is not rejected with unique constraint violated but will replace the old document.   (optional)
    * @tparam T document type
    */
  def insert[T: VPackEncoder: VPackDecoder](
      document: T,
      waitForSync: Boolean = false,
      returnNew: Boolean = false,
      returnOld: Boolean = false,
      silent: Boolean = false,
      overwrite: Boolean = false,
  ): F[ArangoResponse[Document[T]]]

  def indexes(): F[ArangoResponse[IndexList]]
}

object ArangoCollection {
  def apply[F[_]: ArangoProtocol](_database: ArangoDatabase[F], _name: CollectionName): ArangoCollection[F] =
    new ArangoCollection[F] {
      override def database: ArangoDatabase[F] = _database
      override def name: CollectionName = _name

      override def document(key: DocumentKey): ArangoDocument[F] = ArangoDocument(this, key)

      override def checksum(withRevisions: Boolean, withData: Boolean): F[ArangoResponse[CollectionChecksum]] =
        ArangoProtocol[F].execute(
          ArangoRequest.GET(
            database.name,
            s"/_api/collection/$name/checksum",
            Map(
              "withRevisions" -> withRevisions.toString,
              "withData" -> withData.toString,
            )
          )
        )

      override def count(): F[ArangoResponse[CollectionCount]] =
        ArangoProtocol[F].execute(
          ArangoRequest.GET(database.name, s"/_api/collection/$name/count")
        )

      override def info(): F[ArangoResponse[CollectionInfo]] =
        ArangoProtocol[F].execute(
          ArangoRequest.GET(database.name, s"/_api/collection/$name")
        )

      override def revision(): F[ArangoResponse[CollectionRevision]] =
        ArangoProtocol[F].execute(
          ArangoRequest.GET(database.name, s"/_api/collection/$name/revision")
        )

      override def properties(): F[ArangoResponse[CollectionProperties]] =
        ArangoProtocol[F].execute(
          ArangoRequest.GET(database.name, s"/_api/collection/$name/properties")
        )

      override def truncate(): F[ArangoResponse[CollectionInfo]] =
        ArangoProtocol[F].execute(
          ArangoRequest.PUT(
            database.name,
            s"/_api/collection/$name/truncate"
          )
        )

      override def unload(): F[ArangoResponse[CollectionInfo]] =
        ArangoProtocol[F].execute(
          ArangoRequest.PUT(
            database.name,
            s"/_api/collection/$name/unload"
          )
        )

      override def drop(isSystem: Boolean): F[ArangoResponse[CollectionDrop]] =
        ArangoProtocol[F].execute(
          ArangoRequest.DELETE(
            database.name,
            s"/_api/collection/$name",
            Map(
              "isSystem" -> isSystem.toString
            )
          )
        )

      override def create(setup: CollectionCreate => CollectionCreate): F[ArangoResponse[CollectionInfo]] = {
        val options = setup(CollectionCreate(name))
        ArangoProtocol[F].execute(
          ArangoRequest
            .POST(
              database.name,
              s"/_api/collection",
              options.parameters
            )
            .body(options)
        )
      }

      override def insert[T: VPackEncoder: VPackDecoder](
          document: T,
          waitForSync: Boolean,
          returnNew: Boolean,
          returnOld: Boolean,
          silent: Boolean,
          overwrite: Boolean
      ): F[ArangoResponse[Document[T]]] =
        ArangoProtocol[F].execute(
          ArangoRequest
            .POST(
              database.name,
              s"/_api/document/$name",
              Map(
                "waitForSync" -> waitForSync.toString,
                "returnNew" -> returnNew.toString,
                "returnOld" -> returnOld.toString,
                "silent" -> silent.toString,
                "overwrite" -> overwrite.toString,
              )
            )
            .body(document)
        )(
          implicitly[VPackEncoder[T]].mapObject(_.filter(Document.filterEmptyInternalAttributes)),
          implicitly
        )

      override def indexes(): F[ArangoResponse[IndexList]] = ArangoProtocol[F].execute(
        ArangoRequest.GET(
          database.name,
          "/_api/index",
          Map("collection" -> name.repr)
        )
      )
    }
}
