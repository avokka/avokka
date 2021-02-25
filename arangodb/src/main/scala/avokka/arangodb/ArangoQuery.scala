package avokka.arangodb

import avokka.arangodb.api.{Cursor, Query}
import avokka.arangodb.protocol.{ArangoClient, ArangoRequest, ArangoResponse}
import avokka.arangodb.types.DatabaseName
import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.Functor
import cats.syntax.functor._

/**
  * AQL query to be executed
  *
  * @tparam F effect
  * @tparam V bind vars
  */
trait ArangoQuery[F[_], V] {

  /**
    * modify query
    * @param f update function
    * @return modified query
    */
  def withQuery(f: Query[V] => Query[V]): ArangoQuery[F, V]

  def batchSize(value: Long): ArangoQuery[F, V] = withQuery(_.copy(batchSize = Some(value)))
  def count(value: Boolean): ArangoQuery[F, V] = withQuery(_.copy(count = Some(value)))

  /**
    * execute the query
    * @tparam T result document
    * @return response
    */
  def execute[T: VPackDecoder]: F[ArangoResponse[Cursor[T]]]

  /**
    * execute the query
    * @tparam T result document
    * @return cursor
    */
  def cursor[T: VPackDecoder]: F[ArangoCursor[F, T]]

  /**
    * execute the query with streaming handler
    * @param S arango stream implementation
    * @tparam T result document
    * @return stream instance
    */
  def stream[T: VPackDecoder](implicit S: ArangoStream[F]): S.S[F, T]
}

object ArangoQuery {
  def apply[F[_]: ArangoClient : Functor, V: VPackEncoder](
      database: DatabaseName,
      query: Query[V]
  ): ArangoQuery[F, V] = new ArangoQuery[F, V] {

    override def withQuery(f: Query[V] => Query[V]): ArangoQuery[F, V] = apply(database, f(query))

    override def execute[T: VPackDecoder]: F[ArangoResponse[Cursor[T]]] = ArangoClient[F].execute(
      ArangoRequest.POST(database, "/_api/cursor").body(query)
    )

    override def cursor[T: VPackDecoder]: F[ArangoCursor[F, T]] = execute.map { resp =>
      ArangoCursor(database, resp)
    }

    override def stream[T: VPackDecoder](implicit S: ArangoStream[F]): S.S[F, T] = S.fromQuery(this)

  }

}
