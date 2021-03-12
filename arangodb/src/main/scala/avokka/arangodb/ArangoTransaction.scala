package avokka.arangodb

import api._
import types._
import cats.Functor
import cats.syntax.functor._
import protocol._

trait ArangoTransaction[F[_]] {
  def id: String

  def commit(): F[ArangoResponse[Transaction]]
  def abort(): F[ArangoResponse[Transaction]]
}

object ArangoTransaction {
  def apply[F[_] : ArangoClient : Functor](database: DatabaseName, _id: String): ArangoTransaction[F] = new ArangoTransaction[F] {
    override def id: String = _id
    private val api: String = "/_api/transaction/" + id

    override def commit(): F[ArangoResponse[Transaction]] =
      PUT(database, api).execute[F, Result[Transaction]].map(_.result)

    override def abort(): F[ArangoResponse[Transaction]] =
      DELETE(database, api).execute[F, Result[Transaction]].map(_.result)
  }
}