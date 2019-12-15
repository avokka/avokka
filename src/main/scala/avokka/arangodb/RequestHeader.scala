package avokka.arangodb

import avokka.velocypack._

sealed trait RequestHeader

object RequestHeader {

  case class Header
  (
    version: Int = 1,
    `type`: MessageType = MessageType.Request,
    database: DatabaseName,
    requestType: RequestType,
    request: String,
    parameters: Map[String, String] = Map.empty,
    meta: Map[String, String] = Map.empty,
  ) extends RequestHeader

  object Header {
    implicit val encoder: VPackEncoder[Header] = VPackGeneric[Header].encoder
  }

  case class Authentication
  (
    version: Int = 1,
    `type`: MessageType = MessageType.Authentication,
    encryption: String,
    user: String,
    password: String
  ) extends RequestHeader

  object Authentication {
    implicit val encoder: VPackEncoder[Authentication] = VPackGeneric[Authentication].encoder //codec(true)
  }

  implicit val encoder: VPackEncoder[RequestHeader] = {
    case h : Header => Header.encoder.encode(h)
    case h : Authentication => Authentication.encoder.encode(h)
  }

}
