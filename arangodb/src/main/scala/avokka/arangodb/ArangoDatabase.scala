package avokka.arangodb

import api._
import avokka.velocypack._
import protocol._
import types._

trait ArangoDatabase[F[_]] {
  def name: DatabaseName

  def collection(name: CollectionName): ArangoCollection[F]
  def document(handle: DocumentHandle): ArangoDocument[F]

  /**
    * @param users Has to be an array of user objects to initially create for the new database. User information will not be changed for users that already exist. If *users* is not specified or does not contain any users, a default user *root* will be created with an empty string password. This ensures that the new database will be accessible after it is created. Each user object can contain the following attributes:
    */
  def create(users: DatabaseCreate.User*): F[ArangoResponse[DatabaseCreate]]

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

    override def create(users: DatabaseCreate.User*): F[ArangoResponse[DatabaseCreate]] = {
      ArangoProtocol[F].execute(
        ArangoRequest.POST(
          DatabaseName.system,
          "/_api/database"
        ).body(VObject(
          "name" -> name.toVPack,
          "users" -> users.toVPack
        ))
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