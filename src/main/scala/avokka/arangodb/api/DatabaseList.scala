package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack._

case class DatabaseList
(
)

object DatabaseList { self =>

  case class Response
  (
    result: Vector[DatabaseName]
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[Session, DatabaseList, Response] = new Api.EmptyBody[Session, DatabaseList] {
    override type Response = self.Response
    override def requestHeader(session: Session, command: DatabaseList): Request.HeaderTrait = Request.Header(
      database = Database.systemName,
      requestType = RequestType.GET,
      request = "/_api/database/user",
    )
  }
}
