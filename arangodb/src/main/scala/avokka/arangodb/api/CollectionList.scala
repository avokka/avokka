package avokka.arangodb
package api

import avokka.velocypack._

case class CollectionList(
    excludeSystem: Boolean = false
) {
  def parameters = Map(
    "excludeSystem" -> excludeSystem.toString
  )
}

object CollectionList { self =>

  case class Response(
      result: Vector[CollectionInfo.Response]
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoderWithDefaults
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionList, Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionList] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: CollectionList): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = "/_api/collection",
          parameters = command.parameters
        )
    }
}
