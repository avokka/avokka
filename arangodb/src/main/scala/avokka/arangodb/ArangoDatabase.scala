package avokka.arangodb

import models._
import avokka.velocypack._
import cats.Functor
import cats.syntax.functor._
import protocol._
import types._

/**
  * ArangoDB database API
  *
  * @see [[https://www.arangodb.com/docs/stable/http/database-database-management.html]]
  * @tparam F effect
  */
trait ArangoDatabase[F[_]] { self =>

  /** database name */
  def name: DatabaseName

  /** collection api */
  def collection(name: CollectionName): ArangoCollection[F]

  /** document api */
  def document(handle: DocumentHandle): ArangoDocument[F]

  /**
    * List all graphs
    *
    * @return all graphs known to the graph module
    */
  def graphs(): F[ArangoResponse[Vector[GraphInfo]]]

  /** graph api */
  def graph(graphName: String): ArangoGraph[F]

  /** transaction api */
  def transactions: ArangoTransactions[F]

  /** write-ahead log api */
  def wal: ArangoWal[F]

  /**
    * Creates a new database
    *
    * @param users Has to be an array of user objects to initially create for the new database. User information will not be changed for users that already exist. If *users* is not specified or does not contain any users, a default user *root* will be created with an empty string password. This ensures that the new database will be accessible after it is created. Each user object can contain the following attributes:
    */
  def create(users: DatabaseCreate.User*): F[ArangoResponse[Boolean]]

  /**
    * Retrieves the properties of the current database
    *
    * @return database information
    */
  def info(): F[ArangoResponse[DatabaseInfo]]

  /**
    * Drops the database along with all data stored in it.
    *
    * @note dropping a database is only possible from within the _system database. The _system database itself cannot be dropped.
    * @return result
    */
  def drop(): F[ArangoResponse[Boolean]]

  /**
    * Returns all collections
    *
    * @param excludeSystem Whether or not system collections should be excluded from the result
    * @return array of collection informations
    */
  def collections(excludeSystem: Boolean = false): F[ArangoResponse[Vector[CollectionInfo]]]

  /**
    * Builds a AQL query
    *
    * @param query query
    * @tparam V bind type
    * @return query api
    */
  def query[V: VPackEncoder](query: Query[V]): ArangoQuery[F, V]

  /**
    * Builds a AQL query with bind parameters
    *
    * @param qs the query string to be executed
    * @param bindVars key/value pairs representing the bind parameters.
    * @tparam V bind type
    * @return query api
    */
  def query[V: VPackEncoder](qs: String, bindVars: V): ArangoQuery[F, V] = self.query(Query(qs, bindVars))

  /**
    * Build a AQL query with empty bind parameters
    * @param qs the query string to be executed
    * @return query api
    */
  def query(qs: String): ArangoQuery[F, VObject] = self.query(qs, VObject.empty)
}

object ArangoDatabase {
  def apply[F[_]: ArangoClient: Functor](_name: DatabaseName): ArangoDatabase[F] = new ArangoDatabase[F] {

    override val name: DatabaseName = _name

    override def collection(cname: CollectionName): ArangoCollection[F] = ArangoCollection(name, cname)

    override def document(handle: DocumentHandle): ArangoDocument[F] = ArangoDocument(name, handle)

    override def graphs(): F[ArangoResponse[Vector[GraphInfo]]] =
      GET(name, "/_api/gharial")
        .execute[F, GraphList]
        .map(_.map(_.graphs))

    override def graph(graphName: String): ArangoGraph[F] = ArangoGraph(name, graphName)

    override def query[V: VPackEncoder](query: Query[V]): ArangoQuery[F, V] = ArangoQuery(name, query)

    override def collections(excludeSystem: Boolean): F[ArangoResponse[Vector[CollectionInfo]]] =
      GET(name, "/_api/collection", Map("excludeSystem" -> excludeSystem.toString))
        .execute[F, Result[Vector[CollectionInfo]]]
        .map(_.map(_.result))

    override def create(users: DatabaseCreate.User*): F[ArangoResponse[Boolean]] =
      POST(
        DatabaseName.system,
        "/_api/database"
      ).body(
          VObject(
            "name" -> name.toVPack,
            "users" -> users.toVPack
          )
        )
        .execute[F, Result[Boolean]]
        .map(_.map(_.result))

    override def info(): F[ArangoResponse[DatabaseInfo]] =
      GET(name, "/_api/database/current")
        .execute[F, Result[DatabaseInfo]]
        .map(_.map(_.result))

    override def drop(): F[ArangoResponse[Boolean]] =
      DELETE(DatabaseName.system, "/_api/database/" + name)
        .execute[F, Result[Boolean]]
        .map(_.map(_.result))

    override val transactions: ArangoTransactions[F] = ArangoTransactions(name)

    override val wal: ArangoWal[F] = ArangoWal[F](name)
  }
}
