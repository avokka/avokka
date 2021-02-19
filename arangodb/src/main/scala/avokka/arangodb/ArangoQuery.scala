package avokka.arangodb

import avokka.arangodb.api.{Cursor, Query}
import avokka.arangodb.protocol.{ArangoProtocol, ArangoRequest, ArangoResponse}
import avokka.arangodb.types.DatabaseName
import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.Functor
import cats.syntax.functor._

trait ArangoQuery[F[_], V] {
  def execute[T: VPackDecoder]: F[ArangoResponse[Cursor[T]]]
  def cursor[T: VPackDecoder]: F[ArangoCursor[F, T]]
}

object ArangoQuery {
  def apply[F[_]: ArangoProtocol: Functor, V: VPackEncoder](
      database: DatabaseName,
      query: Query[V]
  ): ArangoQuery[F, V] = new ArangoQuery[F, V] {

    override def execute[T: VPackDecoder]: F[ArangoResponse[Cursor[T]]] = ArangoProtocol[F].execute(
      ArangoRequest.POST(database, "/_api/cursor").body(query)
    )

    override def cursor[T: VPackDecoder]: F[ArangoCursor[F, T]] = execute.map { resp =>
      ArangoCursor(database, resp)
    }

  }

  implicit final class AvokkaQueryStreamOps[S[_[_], _], F[_], V](
      private val query: ArangoQuery[F, V]
  )(implicit S: ArangoStream[S, F]) {
    def stream[T: VPackDecoder]: S[F, T] = S.fromQuery(query)
  }

}
