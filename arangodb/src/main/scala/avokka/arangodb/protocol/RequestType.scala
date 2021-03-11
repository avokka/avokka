package avokka.arangodb.protocol

import avokka.velocypack._
import cats.Show

sealed abstract class RequestType(val i: Int) extends Product with Serializable

object RequestType {

  case object DELETE extends RequestType(0)
  case object GET extends RequestType(1)
  case object POST extends RequestType(2)
  case object PUT extends RequestType(3)
  case object HEAD extends RequestType(4)
  case object PATCH extends RequestType(5)
  case object OPTIONS extends RequestType(6)

  implicit val encoder: VPackEncoder[RequestType] = VPackEncoder[Int].contramap(_.i)

  implicit val show: Show[RequestType] = {
    case DELETE => "DELETE"
    case GET => "GET"
    case POST => "POST"
    case PUT => "PUT"
    case HEAD => "HEAD"
    case PATCH => "PATCH"
    case OPTIONS => "OPTIONS"
  }
}
