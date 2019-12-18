package avokka.arangodb.api

import avokka.arangodb.{Collection, Database, Request, RequestType}
import avokka.arangodb.api.CollectionInfo.Response
import avokka.velocypack._

case class CollectionList
(
  excludeSystem: Boolean = false
)
{
  def parameters = Map(
    "excludeSystem" -> excludeSystem.toString
  )
}

object CollectionList { self =>

  case class Response
  (
    result: Vector[CollectionInfo.Response]
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoderWithDefaults
  }

  implicit val api: Api.EmptyBody.Aux[Database, CollectionList, Response] = new Api.EmptyBody[Database, CollectionList] {
    override type Response = self.Response
    override def requestHeader(database: Database, command: CollectionList): Request.HeaderTrait = Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = "/_api/collection",
      parameters = command.parameters
    )
  }
}


