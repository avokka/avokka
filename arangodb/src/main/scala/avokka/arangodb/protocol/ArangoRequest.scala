package avokka.arangodb
package protocol

import avokka.arangodb.types.DatabaseName
import avokka.velocypack._
import cats.Show
import cats.syntax.show._
import shapeless.HNil

final case class ArangoRequest[T](
    header: ArangoRequest.Header,
    body: T
) {
  @inline def execute[F[_]: ArangoClient, O: VPackDecoder](implicit T: VPackEncoder[T]): F[ArangoResponse[O]] = ArangoClient[F].execute[T, O](this)
}

object ArangoRequest {

  sealed trait Header extends Product with Serializable {
    def version: Int = 1
    def `type`: MessageType

    @inline def execute[F[_]: ArangoClient, O: VPackDecoder]: F[ArangoResponse[O]] = ArangoClient[F].execute[O](this)
  }

  sealed trait Request extends Header with Product with Serializable {
    override val `type`: MessageType = MessageType.Request
    def database: DatabaseName
    def requestType: RequestType
    def request: String
    def parameters: Map[String, String]
    def meta: Map[String, String]

    def body[T](value: T): ArangoRequest[T] = ArangoRequest(this, value)
  }

  final case class DELETE(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.DELETE
  }

  final case class GET(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.GET
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

  final case class HEAD(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.HEAD
    @inline def execute[F[_]: ArangoClient]: F[ArangoResponse.Header] = ArangoClient[F].execute(this)
  }

  final case class PATCH(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.PATCH
  }

  final case class OPTIONS(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ) extends Request {
    override def requestType: RequestType = RequestType.OPTIONS
  }

  object Request {
    implicit val encoder: VPackEncoder[Request] = VPackGeneric[Request].cmap { r =>
      r.version :: r.`type` :: r.database :: r.requestType :: r.request :: r.parameters :: r.meta :: HNil
    }

    implicit val show: Show[Request] = { r =>
      show"${r.`type`}(v${r.version},database=${r.database.repr},type=${r.requestType},request=${r.request},parameters=${r.parameters},meta=${r.meta})"
    }
  }

  final case class Authentication(
      override val version: Int = 1,
      override val `type`: MessageType = MessageType.Authentication,
      encryption: String = "plain",
      user: String,
      password: String
  ) extends Header

  object Authentication {
    implicit val encoder: VPackEncoder[Authentication] = VPackGeneric[Authentication].encoder

    implicit val show: Show[Authentication] = { a =>
      show"${a.`type`}(v${a.version},encryption=${a.encryption},user=${a.user},password=${a.password})"
    }
  }

  object Header {
    implicit val encoder: VPackEncoder[Header] = {
      case r: Request        => Request.encoder.encode(r)
      case a: Authentication => Authentication.encoder.encode(a)
    }

    implicit val show: Show[Header] = {
      case r: Request => r.show
      case a: Authentication => a.show
    }
  }
}
