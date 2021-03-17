package avokka.arangodb

import models._
import types._
import cats.Functor
import cats.syntax.functor._
import protocol._

trait ArangoTransaction[F[_]] {
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
  def apply[F[_] : ArangoClient : Functor](database: DatabaseName, _id: TransactionId): ArangoTransaction[F] = new ArangoTransaction[F] {
    override def id: TransactionId = _id
    private val api: String = "/_api/transaction/" + id.repr

    override def status(): F[ArangoResponse[Transaction]] =
      GET(database, api).execute[F, Result[Transaction]].map(_.result)

    override def commit(): F[ArangoResponse[Transaction]] =
      PUT(database, api).execute[F, Result[Transaction]].map(_.result)

    override def abort(): F[ArangoResponse[Transaction]] =
      DELETE(database, api).execute[F, Result[Transaction]].map(_.result)
  }
}