package avokka.arangodb
package api

import avokka.velocypack._

object IndexList { self =>

  case class IndexDetails(
      fields: List[String],
      id: String,
      name: String,
      selectivityEstimate: Double,
      sparse: Boolean,
      `type`: String,
      unique: Boolean,
      deduplicate: Option[Boolean] = None,
  )
  object IndexDetails {
    implicit val decoder: VPackDecoder[IndexDetails] = VPackRecord[IndexDetails].decoderWithDefaults
  }

  case class Response(
      indexes: List[IndexDetails],
      identifiers: Map[String, IndexDetails]
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
          parameters = Map("collection" -> collection.name)
        )
    }
}
