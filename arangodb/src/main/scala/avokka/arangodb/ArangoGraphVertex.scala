package avokka.arangodb

import models._
import protocol._
import types._
import avokka.velocypack._
import cats.Functor
import cats.syntax.functor._

trait ArangoGraphVertex[F[_]] {
  def handle: DocumentHandle

  /**
    * fetches an existing vertex
    *
    * @tparam T          The type of the vertex.
    * @param ifNoneMatch If the "If-None-Match" header is given, then it must contain exactly one
    *                    Etag. The document is returned, if it has a different revision than the
    *                    given Etag. Otherwise an HTTP 304 is returned.
    * @param ifMatch     If the "If-Match" header is given, then it must contain exactly one
    *                    Etag. The document is returned, if it has the same revision as the
    *                    given Etag. Otherwise a HTTP 412 is returned.
    */
  def read[T: VPackDecoder](
    ifNoneMatch: Option[String] = None,
    ifMatch: Option[String] = None,
  ): F[ArangoResponse[T]]
}

object ArangoGraphVertex {
  def apply[F[_] : ArangoClient : Functor](database: DatabaseName, graph: GraphName, _handle: DocumentHandle): ArangoGraphVertex[F] = new ArangoGraphVertex[F] {

    override val handle: DocumentHandle = _handle

    private val path: String = API_GHARIAL + "/" + graph + "/vertex/" + handle.path

    override def read[T: VPackDecoder](ifNoneMatch: Option[String], ifMatch: Option[String]): F[ArangoResponse[T]] =
      GET(database, path, meta = Map(
        "If-None-Match" -> ifNoneMatch,
        "If-Match" -> ifMatch,
      ).collectDefined)
        .execute[F, GraphVertex[T]]
        .map(_.map(_.vertex))
  }
}
