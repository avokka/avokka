package avokka.arangodb
package api

import avokka.velocypack._

object CollectionInfo { self =>

  case class Response(
      id: String,
      name: CollectionName,
      status: CollectionStatus,
      `type`: CollectionType,
      isSystem: Boolean,
      globallyUniqueId: String,
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoderWithDefaults
  }

  implicit val api: Api.EmptyBody.Aux[Collection, CollectionInfo.type, Response] =
    new Api.EmptyBody[Collection, CollectionInfo.type] {
      override type Response = self.Response
      override def header(collection: Collection,
                          command: CollectionInfo.type): Request.HeaderTrait =
        Request.Header(
          database = collection.database.name,
          requestType = RequestType.GET,
          request = s"/_api/collection/${collection.name}",
        )
    }
}
