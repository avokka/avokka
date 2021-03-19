package avokka.arangodb

import models._
import types._
import cats.Functor
import cats.syntax.functor._
import protocol._

/**
  * ArangoDB stream transaction API
  *
  * @see [[https://www.arangodb.com/docs/stable/http/transaction-stream-transaction.html]]
  * @tparam F effect
  */
trait ArangoTransaction[F[_]] {

  /** identifier of the transaction */
  def id: TransactionId

  /**
    * Fetch status of a server-side transaction
    */
  def status(): F[ArangoResponse[Transaction]]

  /**
    * Commit a running server-side transaction. Committing is an idempotent operation. It is not an error to commit a transaction more than once.
    */
  def commit(): F[ArangoResponse[Transaction]]

  /**
    * Abort a running server-side transaction. Aborting is an idempotent operation. It is not an error to abort a transaction more than once.
    * @return
    */
  def abort(): F[ArangoResponse[Transaction]]
}

object ArangoTransaction {
  def apply[F[_]: ArangoClient: Functor](database: DatabaseName, _id: TransactionId): ArangoTransaction[F] =
    new ArangoTransaction[F] {
      override val id: TransactionId = _id
      private val path: String = "/_api/transaction/" + id.repr

      override def status(): F[ArangoResponse[Transaction]] =
        GET(database, path).execute[F, Result[Transaction]].map(_.result)

      override def commit(): F[ArangoResponse[Transaction]] =
        PUT(database, path).execute[F, Result[Transaction]].map(_.result)

      override def abort(): F[ArangoResponse[Transaction]] =
        DELETE(database, path).execute[F, Result[Transaction]].map(_.result)
    }
}
