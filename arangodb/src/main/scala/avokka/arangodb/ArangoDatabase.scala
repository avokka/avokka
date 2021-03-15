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
  def create(users: DatabaseCreate.User*): F[ArangoResponse[Boolean]]

  def info(): F[ArangoResponse[DatabaseInfo]]
  def drop(): F[ArangoResponse[Boolean]]

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

  /**
    * begin a server-side transaction
    *
    * Collections that will be written to in the transaction must be declared with the write or exclusive attribute or it will fail, whereas non-declared collections from which is solely read will be added lazily. See locking and isolation for more information.
    *
    * @param read collections read
    * @param write collections write
    * @param exclusive collections exclusive
    * @param waitForSync an optional boolean flag that, if set, will force the transaction to write all data to disk before returning
    * @param allowImplicit Allow reading from undeclared collections.
    * @param lockTimeout an optional numeric value that can be used to set a timeout for waiting on collection locks. If not specified, a default value will be used. Setting lockTimeout to 0 will make ArangoDB not time out waiting for a lock.
    * @param maxTransactionSize Transaction size limit in bytes. Honored by the RocksDB storage engine only.
    * @return
    */
  def begin(
      read: Seq[CollectionName] = Seq.empty,
      write: Seq[CollectionName] = Seq.empty,
      exclusive: Seq[CollectionName] = Seq.empty,
      waitForSync: Boolean = false,
      allowImplicit: Option[Boolean] = None,
      lockTimeout: Option[Int] = None,
      maxTransactionSize: Option[Long] = None,
  ): F[ArangoTransaction[F]]
}

object ArangoDatabase {
  def apply[F[_]: ArangoClient: Functor](_name: DatabaseName): ArangoDatabase[F] = new ArangoDatabase[F] {

    override def name: DatabaseName = _name

    override def collection(cname: CollectionName): ArangoCollection[F] = ArangoCollection(name, cname)

    override def document(handle: DocumentHandle): ArangoDocument[F] = ArangoDocument(name, handle)

    override def query[V: VPackEncoder](query: Query[V]): ArangoQuery[F, V] = ArangoQuery(name, query)

    override def collections(excludeSystem: Boolean): F[ArangoResponse[Vector[CollectionInfo]]] =
      GET(name, "/_api/collection", Map("excludeSystem" -> excludeSystem.toString))
        .execute[F, Result[Vector[CollectionInfo]]]
        .map(_.result)

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
        .map(_.result)

    override def info(): F[ArangoResponse[DatabaseInfo]] =
      GET(name, "/_api/database/current").execute[F, Result[DatabaseInfo]].map(_.result)

    override def drop(): F[ArangoResponse[Boolean]] =
      DELETE(DatabaseName.system, "/_api/database/" + name).execute[F, Result[Boolean]].map(_.result)

    override def begin(
        read: Seq[CollectionName],
        write: Seq[CollectionName],
        exclusive: Seq[CollectionName],
        waitForSync: Boolean,
        allowImplicit: Option[Boolean],
        lockTimeout: Option[Int],
        maxTransactionSize: Option[Long],
    ): F[ArangoTransaction[F]] =
      POST(name, "/_api/transaction/begin")
        .body(
          VObject(
            "collections" -> VObject(
              "read" -> read.toVPack,
              "write" -> write.toVPack,
              "exclusive" -> exclusive.toVPack,
            ),
            "waitForSync" -> waitForSync.toVPack,
            "allowImplicit" -> allowImplicit.toVPack,
            "lockTimeout" -> lockTimeout.toVPack,
            "maxTransactionSize" -> maxTransactionSize.toVPack,
          )
        )
        .execute[F, Result[Transaction]]
        .map { t =>
          ArangoTransaction(name, t.body.result.id)
        }
  }
}
