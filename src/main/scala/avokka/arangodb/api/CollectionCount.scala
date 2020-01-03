package avokka.arangodb
package api

import avokka.velocypack._

object CollectionCount { self =>

  case class Response(
      count: Long,
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[ArangoCollection, CollectionCount.type, Response] =
    new Api.EmptyBody[ArangoCollection, CollectionCount.type] {
      override type Response = self.Response

      override def header(collection: ArangoCollection, command: CollectionCount.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = collection.database.name,
        requestType = RequestType.GET,
        request = s"/_api/collection/${collection.name}/count",
      )
    }
}
