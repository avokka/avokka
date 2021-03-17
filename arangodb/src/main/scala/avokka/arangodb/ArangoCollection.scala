package avokka.arangodb

import models._
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
    * documents api
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

  def info(): F[ArangoResponse[CollectionInfo]]

  def revision(): F[ArangoResponse[CollectionRevision]]

  def properties(): F[ArangoResponse[CollectionProperties]]

  def unload(): F[ArangoResponse[CollectionInfo]]

  def truncate(): F[ArangoResponse[CollectionInfo]]

  def drop(isSystem: Boolean = false): F[ArangoResponse[DeleteResult]]

  def rename(newName: CollectionName): F[ArangoResponse[CollectionInfo]]

  /**
    * Query all documents in collection
    * @return query
    */
  def all: ArangoQuery[F, VObject]

  def indexes: ArangoIndexes[F]
  def index(id: String): ArangoIndex[F]
}

object ArangoCollection {

  def apply[F[_]: ArangoClient: Functor](database: DatabaseName, _name: CollectionName): ArangoCollection[F] =
    new ArangoCollection[F] {
      override def name: CollectionName = _name

      private val api: String = "/_api/collection/" + name.repr

      override def document(key: DocumentKey): ArangoDocument[F] = ArangoDocument(database, DocumentHandle(name, key))

      override def documents: ArangoDocuments[F] = ArangoDocuments(database, _name)

      override def index(id: String): ArangoIndex[F] = ArangoIndex(database, id)

      override def indexes: ArangoIndexes[F] = ArangoIndexes(database, _name)

      override def checksum(withRevisions: Boolean, withData: Boolean): F[ArangoResponse[CollectionChecksum]] =
          GET(
            database,
            api + "/checksum",
            Map(
              "withRevisions" -> withRevisions.toString,
              "withData" -> withData.toString,
            )
          ).execute

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

      override def all: ArangoQuery[F, VObject] =
        ArangoQuery(database,
                    Query[VObject](
                      query = "FOR doc IN @@collection RETURN doc",
                      bindVars = VObject("@collection" -> name.toVPack)
                    ))

    }

}
