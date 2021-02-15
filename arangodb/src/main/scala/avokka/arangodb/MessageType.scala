package avokka.arangodb

import avokka.velocypack.{VPackDecoder, VPackEncoder, VPackError}

sealed abstract class MessageType(val i: Int) extends Product with Serializable

object MessageType {

  case object Request extends MessageType(1)
  case object ResponseFinal extends MessageType(2)
  case object ResponseChunk extends MessageType(3)
  case object Authentication extends MessageType(1000)

  implicit val encoder: VPackEncoder[MessageType] = VPackEncoder[Int].contramap(_.i)
  implicit val decoder: VPackDecoder[MessageType] = VPackDecoder[Int].flatMap {
    case Request.i        => Right(Request)
    case ResponseFinal.i  => Right(ResponseFinal)
    case ResponseChunk.i  => Right(ResponseChunk)
    case Authentication.i => Right(Authentication)
    case i                => Left(VPackError.IllegalValue(s"unknown message type $i"))
  }
}
