package avokka.arangodb
package api

case class IndexRead(
    handle: String
)

object IndexRead {
  implicit val api: Api.EmptyBody.Aux[Database, IndexRead, Index.Response] =
    new Api.EmptyBody[Database, IndexRead] {
      override type Response = Index.Response
      override def header(database: Database, command: IndexRead): Request.HeaderTrait =
        Request.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = s"/_api/index/${command.handle}"
        )
    }
}
