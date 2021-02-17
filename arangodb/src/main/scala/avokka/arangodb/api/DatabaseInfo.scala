package avokka.arangodb
package api

import avokka.velocypack._
import types._

object DatabaseInfo { self =>

  final case class Response(
      result: Result,
  )

  final case class Result(
      name: DatabaseName,
      id: String,
      path: String,
      isSystem: Boolean,
  )

  object Result {
    implicit val decoder: VPackDecoder[Result] = VPackDecoder.gen
  }

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

/*  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, DatabaseInfo.type, Response] =
    new Api.EmptyBody[ArangoDatabase, DatabaseInfo.type] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: DatabaseInfo.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = database.name,
        requestType = RequestType.GET,
        request = "/_api/database/current"
      )
    }*/
}
