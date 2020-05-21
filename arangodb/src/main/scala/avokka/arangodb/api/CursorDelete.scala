package avokka.arangodb
package api

import avokka.velocypack._

final case class CursorDelete(
    id: String
)

object CursorDelete { self =>

  final case class Response(
      id: String
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CursorDelete, Response] =
    new Api.EmptyBody[ArangoDatabase, CursorDelete] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: CursorDelete): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.DELETE,
          request = s"/_api/cursor/${command.id}"
        )
    }
}
