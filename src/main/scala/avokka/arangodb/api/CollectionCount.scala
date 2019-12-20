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

  implicit val api: Api.EmptyBody.Aux[Collection, CollectionCount.type, Response] =
    new Api.EmptyBody[Collection, CollectionCount.type] {
      override type Response = self.Response

      override def header(collection: Collection, command: CollectionCount.type): Request.HeaderTrait = Request.Header(
        database = collection.database.name,
        requestType = RequestType.GET,
        request = s"/_api/collection/${collection.name}/count",
      )
    }
}
