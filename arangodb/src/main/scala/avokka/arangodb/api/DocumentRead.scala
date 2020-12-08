package avokka.arangodb
package api

import types._

/**
  * Returns the document identified by *document-handle*. The returned document contains three special attributes:
  * *_id* containing the document handle, *_key* containing key which uniquely identifies a document
  * in a given collection and *_rev* containing the revision.
  *
  * @param handle      The handle of the document.
  * @param ifNoneMatch If the "If-None-Match" header is given, then it must contain exactly one
  *                    Etag. The document is returned, if it has a different revision than the
  *                    given Etag. Otherwise an HTTP 304 is returned.
  * @param ifMatch     If the "If-Match" header is given, then it must contain exactly one
  *                    Etag. The document is returned, if it has the same revision as the
  *                    given Etag. Otherwise a HTTP 412 is returned.
  */
final case class DocumentRead[T](
    handle: DocumentHandle,
    ifNoneMatch: Option[String] = None,
    ifMatch: Option[String] = None,
) {
  def meta: Map[String, String] = {
    val m = Map.newBuilder[String, String]
    ifNoneMatch.foreach(m += "If-None-Match" -> _)
    ifMatch.foreach(m += "If-Match" -> _)
    m.result()
  }
}

object DocumentRead {

  implicit def api[T]: Api.EmptyBody.Aux[ArangoDatabase, DocumentRead[T], T] = new Api.EmptyBody[ArangoDatabase, DocumentRead[T]] {
    override type Response = T

    override def header(database: ArangoDatabase, command: DocumentRead[T]): ArangoRequest.HeaderTrait = ArangoRequest.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/document/${command.handle.path}",
      meta = command.meta
    )
  }

}
