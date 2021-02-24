package avokka.arangodb
package protocol

import avokka.arangodb.types.DatabaseName
import avokka.velocypack._
import shapeless.{HNil, ::}

final case class ArangoRequest[T](
    header: ArangoRequest.Header,
    body: T
)

object ArangoRequest {

  sealed trait Header extends Product with Serializable

  sealed trait Request extends Header with Product with Serializable {
    def version: Int = 1
    def `type`: MessageType = MessageType.Request
    def database: DatabaseName
    def requestType: RequestType
    def request: String
    def parameters: Map[String, String]
    def meta: Map[String, String]

    def body[T](value: T): ArangoRequest[T] = ArangoRequest(this, value)
  }

  final case class GET(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.GET
  }

  final case class DELETE(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.DELETE
  }

  final case class POST(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.POST
  }

  final case class PUT(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.PUT
  }

  final case class PATCH(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.PATCH
  }

  object Request {
    implicit val encoder: VPackEncoder[Request] = VPackGeneric[Request].cmap { r =>
      r.version :: r.`type` :: r.database :: r.requestType :: r.request :: r.parameters :: r.meta :: HNil
    }
  }

  final case class Authentication(
      version: Int = 1,
      `type`: MessageType = MessageType.Authentication,
      encryption: String = "plain",
      user: String,
      password: String
  ) extends Header

  object Authentication {
    implicit val encoder: VPackEncoder[Authentication] = VPackGeneric[Authentication].encoder
  }

  object Header {
   // implicit val encoder: VPackEncoder[Header] = VPackEncoder.gen

    implicit val encoder: VPackEncoder[Header] = {
      case r: Request => Request.encoder.encode(r)
      case a: Authentication => Authentication.encoder.encode(a)
    }
  }
}
