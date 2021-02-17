package avokka.arangodb
package api

import avokka.velocypack._
import types._

final case class CollectionInfo(
    name: CollectionName
)

object CollectionInfo { self =>

  final case class Response(
      id: String,
      name: CollectionName,
      status: CollectionStatus,
      `type`: CollectionType,
      isSystem: Boolean,
      globallyUniqueId: String,
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

/*  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionInfo, Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionInfo] {
      override type Response = self.Response
      override def header(database: ArangoDatabase,
                          command: CollectionInfo): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = s"/_api/collection/${command.name}",
        )
    }*/
}
