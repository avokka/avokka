package avokka.arangodb
package api

import avokka.velocypack._

final case class CollectionDrop(
    name: CollectionName,
    isSystem: Boolean = false
) {
  def parameters = Map(
    "isSystem" -> isSystem.toString
  )
}

object CollectionDrop { self =>

  final case class Response(
      id: String
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionDrop, Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionDrop] {
      override type Response = self.Response
      override def header(database: ArangoDatabase,
                          command: CollectionDrop): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.DELETE,
          request = s"/_api/collection/${command.name}",
          parameters = command.parameters
        )
    }
}
