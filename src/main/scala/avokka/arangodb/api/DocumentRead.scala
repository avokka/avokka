package avokka.arangodb
package api

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
case class DocumentRead[T]
(
  handle: DocumentHandle,
  ifNoneMatch: Option[String] = None,
  ifMatch: Option[String] = None,
) {
  def meta: Map[String, String] = {
    Map(
      "If-None-Match" -> ifNoneMatch,
      "If-Match" -> ifMatch
    ).flatMap { case (k, v) => v.map(vv => k -> vv) }
  }
}

object DocumentRead {

  implicit def api[T]: Api.EmptyBody.Aux[Database, DocumentRead[T], T] = new Api.EmptyBody[Database, DocumentRead[T]] {
    override type Response = T

    override def header(database: Database, command: DocumentRead[T]): Request.HeaderTrait = Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/document/${command.handle.path}",
      meta = command.meta
    )
  }

}
