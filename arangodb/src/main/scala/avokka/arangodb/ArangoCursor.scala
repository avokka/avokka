package avokka.arangodb

import avokka.arangodb.models._
import avokka.arangodb.protocol._
import avokka.arangodb.types.DatabaseName
import avokka.velocypack.VPackDecoder
import cats.Functor
import cats.syntax.functor._

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
            s"/_api/cursor/${body.id.get}",
            meta = Map(
              Transaction.KEY -> options.transaction.map(_.repr)
            ).collectDefined
          )
        )
        .map { apply(database, _, options) }

    override def delete(): F[ArangoResponse[DeleteResult]] =
      DELETE(
        database,
        s"/_api/cursor/${body.id.get}",
        meta = Map(
          Transaction.KEY -> options.transaction.map(_.repr)
        ).collectDefined
      ).execute

  }
}
