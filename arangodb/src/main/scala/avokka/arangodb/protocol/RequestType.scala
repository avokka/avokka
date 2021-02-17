package avokka.arangodb.protocol

import avokka.velocypack.{VPackDecoder, VPackEncoder, VPackError}

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
  implicit val decoder: VPackDecoder[RequestType] = VPackDecoder[Int].flatMap {
    case DELETE.i  => Right(DELETE)
    case GET.i     => Right(GET)
    case POST.i    => Right(POST)
    case PUT.i     => Right(PUT)
    case HEAD.i    => Right(HEAD)
    case PATCH.i   => Right(PATCH)
    case OPTIONS.i => Right(OPTIONS)
    case i         => Left(VPackError.IllegalValue(s"unknown request type $i"))
  }
}
