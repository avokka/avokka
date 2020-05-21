package avokka.arangodb
package api

final case class IndexRead(
    handle: String
)

object IndexRead {
  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, IndexRead, Index.Response] =
    new Api.EmptyBody[ArangoDatabase, IndexRead] {
      override type Response = Index.Response
      override def header(database: ArangoDatabase, command: IndexRead): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = s"/_api/index/${command.handle}"
        )
    }
}
