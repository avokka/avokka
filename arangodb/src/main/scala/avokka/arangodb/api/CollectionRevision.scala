package avokka.arangodb
package api

import avokka.velocypack._

case class CollectionRevision(
    name: CollectionName
)

object CollectionRevision { self =>

  case class Response(
      revision: String,
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoderWithDefaults
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionRevision, Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionRevision] {
      override type Response = self.Response
      override def header(database: ArangoDatabase,
                          command: CollectionRevision): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = s"/_api/collection/${command.name}/revision",
        )
    }
}
