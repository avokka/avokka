package avokka.arangodb
package api

import avokka.velocypack._

case class CollectionDrop(
    isSystem: Boolean = false
) {
  def parameters = Map(
    "isSystem" -> isSystem.toString
  )
}

object CollectionDrop { self =>

  case class Response(
      id: String
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[ArangoCollection, CollectionDrop, Response] =
    new Api.EmptyBody[ArangoCollection, CollectionDrop] {
      override type Response = self.Response
      override def header(collection: ArangoCollection, command: CollectionDrop): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = collection.database.name,
          requestType = RequestType.DELETE,
          request = s"/_api/collection/${collection.name}",
          parameters = command.parameters
        )
    }
}
