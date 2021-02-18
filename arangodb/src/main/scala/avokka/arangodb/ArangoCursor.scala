package avokka.arangodb

import avokka.arangodb.api._
import avokka.arangodb.protocol.{ArangoProtocol, ArangoRequest, ArangoResponse}
import avokka.arangodb.types.DatabaseName
import avokka.velocypack.VPackDecoder
import cats.Functor
import cats.syntax.functor._

trait ArangoCursor[F[_], T] {
  def header: ArangoResponse.Header
  def body: Cursor[T]
  def next(): F[ArangoCursor[F, T]]
  def delete(): F[ArangoResponse[CursorDelete]]
}

object ArangoCursor {
  def apply[F[_]: ArangoProtocol: Functor, T: VPackDecoder](
      database: DatabaseName,
      response: ArangoResponse[Cursor[T]]
  ): ArangoCursor[F, T] = new ArangoCursor[F, T] {

    override def header: ArangoResponse.Header = response.header
    override def body: Cursor[T] = response.body

    override def next(): F[ArangoCursor[F, T]] =
      ArangoProtocol[F]
        .execute[Cursor[T]](
          ArangoRequest.PUT(
            database,
            s"/_api/cursor/${body.id.get}"
          )
        )
        .map { response =>
          apply(database, response)
        }

    override def delete(): F[ArangoResponse[CursorDelete]] = ArangoProtocol[F].execute(
      ArangoRequest.DELETE(
        database,
        s"/_api/cursor/${body.id.get}"
      )
    )
  }
}
