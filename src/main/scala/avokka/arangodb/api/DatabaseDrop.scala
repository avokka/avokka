package avokka.arangodb
package api

import avokka.velocypack._

object DatabaseDrop { self =>

  case class Response
  (
    result: Boolean
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[Database, DatabaseDrop.type, Response] = new Api.EmptyBody[Database, DatabaseDrop.type] {
    override type Response = self.Response
    override def requestHeader(database: Database, command: DatabaseDrop.type): Request.HeaderTrait = Request.Header(
      database = Database.systemName,
      requestType = RequestType.DELETE,
      request = s"/_api/database/${database.name}",
    )
  }
}
