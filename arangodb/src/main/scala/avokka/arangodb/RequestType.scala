package avokka.arangodb

import avokka.velocypack.{VPackDecoder, VPackEncoder, VPackError}
import cats.syntax.either._

sealed abstract class RequestType(val i: Int) extends Product

object RequestType {

  case object DELETE extends RequestType(0)
  case object GET extends RequestType(1)
  case object POST extends RequestType(2)
  case object PUT extends RequestType(3)
  case object HEAD extends RequestType(4)
  case object PATCH extends RequestType(5)
  case object OPTIONS extends RequestType(6)

  implicit val encoder: VPackEncoder[RequestType] = VPackEncoder[Int].contramap(_.i)
  implicit val decoder: VPackDecoder[RequestType] = VPackDecoder[Int].emap {
    case DELETE.i  => DELETE.asRight
    case GET.i     => GET.asRight
    case POST.i    => POST.asRight
    case PUT.i     => PUT.asRight
    case HEAD.i    => HEAD.asRight
    case PATCH.i   => PATCH.asRight
    case OPTIONS.i => OPTIONS.asRight
    case i         => VPackError.IllegalValue(s"unknown request type $i").asLeft
  }
}
