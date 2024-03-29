package avokka.arangodb

import avokka.arangodb.models._
import avokka.arangodb.protocol._
import avokka.arangodb.types._
import avokka.velocypack.VPackDecoder
import cats.Functor
import cats.syntax.functor._

/**
  * Arango cursor API
  *
  * @tparam F effect
  * @see [[https://www.arangodb.com/docs/stable/http/aql-query-cursor-accessing-cursors.html]]
  */
trait ArangoCursor[F[_], T] {
  def header: ArangoResponse.Header
  def body: Cursor[T]
  def next(): F[ArangoCursor[F, T]]
  def delete(): F[ArangoResponse[DeleteResult]]
}

object ArangoCursor {

  def apply[F[_]: ArangoClient: Functor, T: VPackDecoder](
      database: DatabaseName,
      response: ArangoResponse[Cursor[T]],
      options: ArangoQuery.Options
  ): ArangoCursor[F, T] = new ArangoCursor[F, T] {

    override val header: ArangoResponse.Header = response.header
    override val body: Cursor[T] = response.body

    override def next(): F[ArangoCursor[F, T]] =
      ArangoClient[F]
        .execute[Cursor[T]](
          PUT(
            database,
            API_CURSOR + "/" + body.id.get,
            meta = Map(
              Transaction.KEY -> options.transaction.map(_.repr)
            ).collectDefined
          )
        )
        .map { apply(database, _, options) }

    override def delete(): F[ArangoResponse[DeleteResult]] =
      DELETE(
        database,
        API_CURSOR + "/" + body.id.get,
        meta = Map(
          Transaction.KEY -> options.transaction.map(_.repr)
        ).collectDefined
      ).execute

  }
}
