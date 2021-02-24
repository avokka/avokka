package avokka.arangodb

import avokka.arangodb.api._
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
  def apply[F[_]: ArangoClient: Functor, T: VPackDecoder, S](
      database: DatabaseName,
      response: ArangoResponse[Cursor[T]]
  ): ArangoCursor[F, T] = new ArangoCursor[F, T] {

    override def header: ArangoResponse.Header = response.header
    override def body: Cursor[T] = response.body

    override def next(): F[ArangoCursor[F, T]] =
      ArangoClient[F]
        .execute[Cursor[T]](
          ArangoRequest.PUT(
            database,
            s"/_api/cursor/${body.id.get}"
          )
        )
        .map { apply(database, _) }

    override def delete(): F[ArangoResponse[DeleteResult]] = ArangoClient[F].execute(
      ArangoRequest.DELETE(
        database,
        s"/_api/cursor/${body.id.get}"
      )
    )
  }
}
