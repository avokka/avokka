package avokka.arangodb

import api._
import avokka.velocypack._
import cats.Functor
import cats.syntax.functor._
import protocol._
import types._

trait ArangoDatabase[F[_]] { self =>
  def name: DatabaseName

  def collection(name: CollectionName): ArangoCollection[F]
  def document(handle: DocumentHandle): ArangoDocument[F]

  /**
    * @param users Has to be an array of user objects to initially create for the new database. User information will not be changed for users that already exist. If *users* is not specified or does not contain any users, a default user *root* will be created with an empty string password. This ensures that the new database will be accessible after it is created. Each user object can contain the following attributes:
    */
  def create(users: DatabaseCreate.User*): F[ArangoResponse[DatabaseResult]]

  def info(): F[ArangoResponse[DatabaseInfo]]
  def drop(): F[ArangoResponse[DatabaseResult]]

  def collections(excludeSystem: Boolean = false): F[ArangoResponse[Vector[CollectionInfo]]]

  /**
    * @param qs contains the query string to be executed
    * @param bindVars key/value pairs representing the bind parameters.
    */
  def query[V: VPackEncoder](
      qs: String,
      bindVars: V,
  ): ArangoQuery[F, V] = self.query(Query(qs, bindVars))

  def query(qs: String): ArangoQuery[F, VObject] = query(qs, VObject.empty)
  def query[V: VPackEncoder](query: Query[V]): ArangoQuery[F, V]
}

object ArangoDatabase {
  def apply[F[_]: ArangoClient : Functor](_name: DatabaseName): ArangoDatabase[F] = new ArangoDatabase[F] {

    override def name: DatabaseName = _name

    override def collection(cname: CollectionName): ArangoCollection[F] = ArangoCollection(name, cname)

    override def document(handle: DocumentHandle): ArangoDocument[F] = ArangoDocument(name, handle)

    override def query[V: VPackEncoder](query: Query[V]): ArangoQuery[F, V] = ArangoQuery(name, query)

    override def collections(excludeSystem: Boolean): F[ArangoResponse[Vector[CollectionInfo]]] =
      GET(name, "/_api/collection", Map("excludeSystem" -> excludeSystem.toString))
        .execute[F, Result[Vector[CollectionInfo]]].map(_.result)

    override def create(users: DatabaseCreate.User*): F[ArangoResponse[DatabaseResult]] =
      POST(
        DatabaseName.system,
        "/_api/database"
      )
      .body(
        VObject(
          "name" -> name.toVPack,
          "users" -> users.toVPack
        )
      ).execute

    override def info(): F[ArangoResponse[DatabaseInfo]] =
      GET(name, "/_api/database/current").execute[F, Result[DatabaseInfo]].map(_.result)

    override def drop(): F[ArangoResponse[DatabaseResult]] =
      DELETE(DatabaseName.system, "/_api/database/" + name).execute

  }
}
