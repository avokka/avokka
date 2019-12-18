package avokka.arangodb.api

import avokka.arangodb.{Collection, Request, RequestType}
import avokka.velocypack._

case class CollectionDrop
(
  isSystem: Boolean = false
)
{
  def parameters = Map(
    "isSystem" -> isSystem.toString
  )
}

object CollectionDrop { self =>

  case class Response
  (
    id: String
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[Collection, CollectionDrop, Response] = new Api.EmptyBody[Collection, CollectionDrop] {
    override type Response = self.Response
    override def requestHeader(collection: Collection, command: CollectionDrop): Request.HeaderTrait = Request.Header(
      database = collection.database.name,
      requestType = RequestType.DELETE,
      request = s"/_api/collection/${collection.name}",
      parameters = command.parameters
    )
  }
}


