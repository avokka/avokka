package avokka.arangodb
package api

import avokka.velocypack._

case class IndexDelete(
    handle: String
)

object IndexDelete { self =>

  case class Response(
      id: String
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[Database, IndexDelete, Response] =
    new Api.EmptyBody[Database, IndexDelete] {
      override type Response = self.Response

      override def header(database: Database, command: IndexDelete): Request.HeaderTrait =
        Request.Header(
          database = database.name,
          requestType = RequestType.DELETE,
          request = s"/_api/index/${command.handle}"
        )
    }
}
