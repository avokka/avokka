package avokka.arangodb
package api

import avokka.velocypack._

object CollectionRevision { self =>

  case class Response(
      revision: String,
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoderWithDefaults
  }

  implicit val api: Api.EmptyBody.Aux[ArangoCollection, CollectionRevision.type, Response] =
    new Api.EmptyBody[ArangoCollection, CollectionRevision.type] {
      override type Response = self.Response
      override def header(collection: ArangoCollection,
                          command: CollectionRevision.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = collection.database.name,
        requestType = RequestType.GET,
        request = s"/_api/collection/${collection.name}/revision",
      )
    }
}
