package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack._
import scodec.Codec

object DatabaseInfo { self =>

  case class Response
  (
    result: Result,
  )

  case class Result
  (
    name: DatabaseName,
    id: String,
    path: String,
    isSystem: Boolean,
  )

  object Result {
    implicit val decoder: VPackDecoder[Result] = VPackRecord[Result].decoder
  }

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[Database, DatabaseInfo.type, Response] = new Api.EmptyBody[Database, DatabaseInfo.type] {
    override type Response = self.Response
    override def requestHeader(database: Database, command: DatabaseInfo.type): Request.HeaderTrait = Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = "/_api/database/current"
    )
  }
}
