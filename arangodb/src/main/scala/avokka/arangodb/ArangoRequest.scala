package avokka.arangodb

import avokka.arangodb.api.Api
import avokka.arangodb.types.DatabaseName
import avokka.velocypack._

final case class ArangoRequest[T]
(
  header: ArangoRequest.Header,
  body: T
)

object ArangoRequest {

  final case class Header
  (
    version: Int = 1,
    `type`: MessageType = MessageType.Request,
    database: DatabaseName,
    requestType: RequestType,
    request: String,
    parameters: Map[String, String] = Map.empty,
    meta: Map[String, String] = Map.empty,
  )

  def GET(
    database: DatabaseName,
    request: String,
    parameters: Map[String, String] = Map.empty,
    meta: Map[String, String] = Map.empty
  ): Header = Header(
    database = database,
    requestType = RequestType.GET,
    request = request,
    parameters = parameters,
    meta = meta
  )

  object Header {
    implicit val encoder: VPackEncoder[Header] = VPackGeneric[Header].encoder
  }

  final case class Authentication
  (
    version: Int = 1,
    `type`: MessageType = MessageType.Authentication,
    encryption: String = "plain",
    user: String,
    password: String
  )

  object Authentication {
    implicit val encoder: VPackEncoder[Authentication] = VPackGeneric[Authentication].encoder

    implicit val api: Api.EmptyBody.Aux[ArangoSession, Authentication, ResponseError] =
      new Api.EmptyBody[ArangoSession, Authentication] {
        override type Response = ResponseError
        override def header(session: ArangoSession, command: Authentication): HeaderTrait = command
      }
  }

}
