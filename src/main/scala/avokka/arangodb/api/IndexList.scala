package avokka.arangodb
package api

import avokka.velocypack._

object IndexList { self =>

  case class Response(
      indexes: List[Index.Response],
      identifiers: Map[String, Index.Response]
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoderWithDefaults
  }

  implicit val api: Api.EmptyBody.Aux[Collection, IndexList.type, Response] =
    new Api.EmptyBody[Collection, IndexList.type] {
      override type Response = self.Response
      override def header(collection: Collection, command: IndexList.type): Request.HeaderTrait =
        Request.Header(
          database = collection.database.name,
          requestType = RequestType.GET,
          request = "/_api/index",
          parameters = Map("collection" -> collection.name.repr)
        )
    }
}
