package avokka.arangodb

import avokka.arangodb.api.Api
import avokka.velocypack._

case class Request[T](
    header: Request.HeaderTrait,
    body: T
)

object Request {

  sealed trait HeaderTrait

  case class Header(
      version: Int = 1,
      `type`: MessageType = MessageType.Request,
      database: DatabaseName,
      requestType: RequestType,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty,
  ) extends HeaderTrait

  object Header {
    implicit val encoder: VPackEncoder[Header] = VPackGeneric[Header].encoder
  }

  case class Authentication(
      version: Int = 1,
      `type`: MessageType = MessageType.Authentication,
      encryption: String = "plain",
      user: String,
      password: String
  ) extends HeaderTrait

  object Authentication {
    implicit val encoder: VPackEncoder[Authentication] = VPackGeneric[Authentication].encoder

    implicit val api: Api.EmptyBody.Aux[Session, Authentication, ResponseError] =
      new Api.EmptyBody[Session, Authentication] {
        override type Response = ResponseError
        override def header(session: Session, command: Authentication): HeaderTrait = command
      }
  }

  implicit val headerEncoder: VPackEncoder[HeaderTrait] = {
    case h: Header         => Header.encoder.encode(h)
    case h: Authentication => Authentication.encoder.encode(h)
  }

}
