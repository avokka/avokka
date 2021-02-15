package avokka.arangodb
package api

import avokka.velocypack._
import types._

object DatabaseDrop { self =>

  final case class Response(
      result: Boolean
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, DatabaseDrop.type, Response] =
    new Api.EmptyBody[ArangoDatabase, DatabaseDrop.type] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: DatabaseDrop.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = DatabaseName.system,
        requestType = RequestType.DELETE,
        request = s"/_api/database/${database.name}",
      )
    }
}
