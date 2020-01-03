package avokka.arangodb
package api

import avokka.velocypack._

case class DatabaseList(
    )

object DatabaseList { self =>

  case class Response(
      result: Vector[DatabaseName]
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[ArangoSession, DatabaseList, Response] = new Api.EmptyBody[ArangoSession, DatabaseList] {
    override type Response = self.Response
    override def header(session: ArangoSession, command: DatabaseList): ArangoRequest.HeaderTrait = ArangoRequest.Header(
      database = DatabaseName.system,
      requestType = RequestType.GET,
      request = "/_api/database/user",
    )
  }
}
