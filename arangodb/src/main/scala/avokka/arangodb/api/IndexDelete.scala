package avokka.arangodb
package api

import avokka.velocypack._

final case class IndexDelete(
    handle: String
)

object IndexDelete { self =>

  final case class Response(
      id: String
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, IndexDelete, Response] =
    new Api.EmptyBody[ArangoDatabase, IndexDelete] {
      override type Response = self.Response

      override def header(database: ArangoDatabase, command: IndexDelete): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.DELETE,
          request = s"/_api/index/${command.handle}"
        )
    }
}
