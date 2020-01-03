package avokka.arangodb
package api

import avokka.velocypack._

object DatabaseInfo { self =>

  case class Response(
      result: Result,
  )

  case class Result(
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

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, DatabaseInfo.type, Response] =
    new Api.EmptyBody[ArangoDatabase, DatabaseInfo.type] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: DatabaseInfo.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = database.name,
        requestType = RequestType.GET,
        request = "/_api/database/current"
      )
    }
}
