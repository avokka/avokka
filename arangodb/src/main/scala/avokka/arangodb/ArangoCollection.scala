package avokka.arangodb

import api._
import protocol._
import types._

trait ArangoCollection[F[_]] {
  def database: ArangoDatabase[F]
  def name: CollectionName

  def document(key: DocumentKey): ArangoDocument[F]

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

  def drop(isSystem: Boolean = false): F[ArangoResponse[CollectionDrop]]
}

object ArangoCollection {
  def apply[F[_]: ArangoProtocol](_database: ArangoDatabase[F],
                                  _name: CollectionName): ArangoCollection[F] =
    new ArangoCollection[F] {
      override def database: ArangoDatabase[F] = _database
      override def name: CollectionName = _name

      override def document(key: DocumentKey): ArangoDocument[F] = ArangoDocument(this, key)

      override def checksum(
          withRevisions: Boolean,
          withData: Boolean): F[ArangoResponse[CollectionChecksum]] =
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

      override def drop(isSystem: Boolean): F[ArangoResponse[CollectionDrop]] = ArangoProtocol[F].execute(
        ArangoRequest.DELETE(database.name, s"/_api/collection/$name",
          Map(
            "isSystem" -> isSystem.toString
          )
        )
      )
    }
}
