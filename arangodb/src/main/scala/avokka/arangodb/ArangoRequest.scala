package avokka.arangodb

import avokka.arangodb.api.Api
import avokka.velocypack._
import types._

final case class ArangoRequest[T](
                             header: ArangoRequest.HeaderTrait,
                             body: T
)

object ArangoRequest {

  sealed trait HeaderTrait extends Product with Serializable

  final case class Header(
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

  final case class Authentication(
      version: Int = 1,
      `type`: MessageType = MessageType.Authentication,
      encryption: String = "plain",
      user: String,
      password: String
  ) extends HeaderTrait

  object Authentication {
    implicit val encoder: VPackEncoder[Authentication] = VPackGeneric[Authentication].encoder

    implicit val api: Api.EmptyBody.Aux[ArangoSession, Authentication, ResponseError] =
      new Api.EmptyBody[ArangoSession, Authentication] {
        override type Response = ResponseError
        override def header(session: ArangoSession, command: Authentication): HeaderTrait = command
      }
  }

  implicit val headerEncoder: VPackEncoder[HeaderTrait] = {
    case h: Header         => Header.encoder.encode(h)
    case h: Authentication => Authentication.encoder.encode(h)
  }

}
