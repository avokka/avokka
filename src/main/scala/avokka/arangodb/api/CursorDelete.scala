package avokka.arangodb
package api

import avokka.velocypack._

case class CursorDelete(
    id: String
)

object CursorDelete { self =>

  case class Response(
      id: String
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[Database, CursorDelete, Response] =
    new Api.EmptyBody[Database, CursorDelete] {
      override type Response = self.Response
      override def header(database: Database, command: CursorDelete): Request.HeaderTrait =
        Request.Header(
          database = database.name,
          requestType = RequestType.DELETE,
          request = s"/_api/cursor/${command.id}"
        )
    }
}
