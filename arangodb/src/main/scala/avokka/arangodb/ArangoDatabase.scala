package avokka.arangodb

import api._
import protocol._
import types._

trait ArangoDatabase[F[_]] {
  def name: DatabaseName

  def collection(name: CollectionName): ArangoCollection[F]
  def document(handle: DocumentHandle): ArangoDocument[F]

  def create(setup: DatabaseCreate => DatabaseCreate = identity): F[ArangoResponse[DatabaseCreate.Response]]

  def info(): F[ArangoResponse[DatabaseInfo]]
  def drop(): F[ArangoResponse[DatabaseDrop]]

  def collections(excludeSystem: Boolean = false): F[ArangoResponse[api.CollectionList]]
}

object ArangoDatabase {
  def apply[F[_]: ArangoProtocol](_name: DatabaseName): ArangoDatabase[F] = new ArangoDatabase[F] {
    override def name: DatabaseName = _name

    override def collection(name: CollectionName): ArangoCollection[F] = ArangoCollection(this, name)
    override def document(handle: DocumentHandle): ArangoDocument[F] = ArangoDocument(this, handle)

    override def collections(excludeSystem: Boolean): F[ArangoResponse[CollectionList]] =
      ArangoProtocol[F].execute(ArangoRequest.GET(
        name,
        "/_api/collection",
        Map(
          "excludeSystem" -> excludeSystem.toString
        )
      ))

    override def create(setup: DatabaseCreate => DatabaseCreate): F[ArangoResponse[DatabaseCreate.Response]] = {
      val options = setup(DatabaseCreate(name))
      ArangoProtocol[F].execute(
        ArangoRequest.POST(
          DatabaseName.system,
          "/_api/database"
        ).body(options)
      )
    }

    override def info(): F[ArangoResponse[DatabaseInfo]] = ArangoProtocol[F].execute(
      ArangoRequest.GET(
        name,
        "/_api/database/current"
      )
    )

    override def drop(): F[ArangoResponse[DatabaseDrop]] = ArangoProtocol[F].execute(
      ArangoRequest.DELETE(
        DatabaseName.system,
        s"/_api/database/$name"
      )
    )
  }
}