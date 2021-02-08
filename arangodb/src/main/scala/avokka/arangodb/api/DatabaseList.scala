package avokka.arangodb
package api

import avokka.velocypack._
import types._

final case class DatabaseList(
    )

object DatabaseList { self =>

  final case class Response(
      result: Vector[DatabaseName]
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
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
