package avokka.arangodb

import models._
import avokka.velocypack._
import cats.Functor
import cats.syntax.functor._
import types._
import protocol._

/**
  * ArangoDB stream transactions API
  *
  * @see https://www.arangodb.com/docs/stable/http/transaction-stream-transaction.html
  * @tparam F effect
  */
trait ArangoTransactions[F[_]] {

  /**
    * Return the currently running server-side transactions
    *
    * @return an object with the attribute transactions, which contains an array of transactions. In a cluster the array will contain the transactions from all Coordinators.
    */
  def list(): F[ArangoResponse[TransactionList]]

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
    * @return transaction api
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

object ArangoTransactions {

  def apply[F[_]: ArangoClient: Functor](database: DatabaseName): ArangoTransactions[F] = new ArangoTransactions[F] {

    override def list(): F[ArangoResponse[TransactionList]] =
      GET(database, "/_api/transaction").execute

    override def begin(
        read: Seq[CollectionName],
        write: Seq[CollectionName],
        exclusive: Seq[CollectionName],
        waitForSync: Boolean,
        allowImplicit: Option[Boolean],
        lockTimeout: Option[Int],
        maxTransactionSize: Option[Long],
    ): F[ArangoTransaction[F]] =
      POST(database, "/_api/transaction/begin")
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
          ArangoTransaction(database, t.body.result.id)
        }
  }
}
