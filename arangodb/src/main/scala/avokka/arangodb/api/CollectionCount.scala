package avokka.arangodb
package api

import avokka.velocypack._

case class CollectionCount
(
  name: CollectionName,
)

object CollectionCount { self =>

  case class Response(
      count: Long,
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionCount, Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionCount] {
      override type Response = self.Response

      override def header(database: ArangoDatabase, command: CollectionCount): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = database.name,
        requestType = RequestType.GET,
        request = s"/_api/collection/${command.name}/count",
      )
    }
}
