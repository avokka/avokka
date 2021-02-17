package avokka.arangodb

import avokka.arangodb.protocol.{ArangoProtocol, ArangoRequest, ArangoResponse}
import avokka.arangodb.types.{DocumentHandle, DocumentKey}
import avokka.velocypack.VPackDecoder

trait ArangoDocument[F[_]] {
  def database: ArangoDatabase[F]
  def handle: DocumentHandle

  /**
    * Returns the document identified by *document-handle*. The returned document contains three special attributes:
    * *_id* containing the document handle, *_key* containing key which uniquely identifies a document
    * in a given collection and *_rev* containing the revision.
    *
    * @tparam T          The type of the document.
    * @param ifNoneMatch If the "If-None-Match" header is given, then it must contain exactly one
    *                    Etag. The document is returned, if it has a different revision than the
    *                    given Etag. Otherwise an HTTP 304 is returned.
    * @param ifMatch     If the "If-Match" header is given, then it must contain exactly one
    *                    Etag. The document is returned, if it has the same revision as the
    *                    given Etag. Otherwise a HTTP 412 is returned.
    */
  def read[T: VPackDecoder](ifNoneMatch: Option[String] = None, ifMatch: Option[String] = None): F[ArangoResponse[T]]
}

object ArangoDocument {

  def apply[F[_]: ArangoProtocol](_database: ArangoDatabase[F], _handle: DocumentHandle): ArangoDocument[F] = new Protocol[F] {
    override def database: ArangoDatabase[F] = _database
    override def handle: DocumentHandle = _handle
  }

  def apply[F[_]: ArangoProtocol](_collection: ArangoCollection[F], _key: DocumentKey): ArangoDocument[F] = new Protocol[F] {
    override def database: ArangoDatabase[F] = _collection.database
    override def handle: DocumentHandle = DocumentHandle(_collection.name, _key)
  }

  abstract class Protocol[F[_]: ArangoProtocol] extends ArangoDocument[F] {
    override def read[T: VPackDecoder](ifNoneMatch: Option[String], ifMatch: Option[String]): F[ArangoResponse[T]] =
      ArangoProtocol[F].execute(ArangoRequest.GET(
        database.name,
        s"/_api/document/${handle.path}",
        meta = Map(
          "If-None-Match" -> ifNoneMatch,
          "If-Match" -> ifMatch
        ).collectDefined
      ))
  }

}
