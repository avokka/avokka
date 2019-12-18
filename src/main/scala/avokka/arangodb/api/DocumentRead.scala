package avokka.arangodb
package api

case class DocumentRead[T]
(
  handle: DocumentHandle
)

object DocumentRead {

  implicit def api[T]: Api.EmptyBody.Aux[Database, DocumentRead[T], T] = new Api.EmptyBody[Database, DocumentRead[T]] {
    override type Response = T
    override def requestHeader(database: Database, command: DocumentRead[T]): Request.HeaderTrait = Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/document/${command.handle.path}"
    )
  }

}
