package avokka.arangodb

import avokka.arangodb.api.CollectionList
import avokka.arangodb.protocol.{ArangoProtocol, ArangoRequest, ArangoResponse}
import avokka.arangodb.types.{CollectionName, DatabaseName, DocumentHandle}

trait ArangoDatabase[F[_]] {
  def name: DatabaseName

  def collection(name: CollectionName): ArangoCollection[F]
  def document(handle: DocumentHandle): ArangoDocument[F]

  def collections(excludeSystem: Boolean = false): F[ArangoResponse[api.CollectionList.Response]]
}

object ArangoDatabase {
  def apply[F[_]: ArangoProtocol](_name: DatabaseName): ArangoDatabase[F] = new ArangoDatabase[F] {
    override def name: DatabaseName = _name

    override def collection(name: CollectionName): ArangoCollection[F] = ArangoCollection(this, name)
    override def document(handle: DocumentHandle): ArangoDocument[F] = ArangoDocument(this, handle)

    override def collections(excludeSystem: Boolean): F[ArangoResponse[CollectionList.Response]] =
      ArangoProtocol[F].execute(ArangoRequest.GET(
        name,
        "/_api/collection",
        Map(
          "excludeSystem" -> excludeSystem.toString
        )
      ))

  }
}