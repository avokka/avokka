package avokka.arangodb

import avokka.velocypack.{VPackDecoder, VPackEncoder, VPackError}
import cats.syntax.either._

sealed abstract class MessageType(val i: Int)

object MessageType {

  case object Request extends MessageType(1)
  case object ResponseFinal extends MessageType(2)
  case object ResponseChunk extends MessageType(3)
  case object Authentication extends MessageType(1000)

  implicit val encoder: VPackEncoder[MessageType] = VPackEncoder[Int].contramap(_.i)
  implicit val decoder: VPackDecoder[MessageType] = VPackDecoder[Int].emap {
    case Request.i        => Request.asRight
    case ResponseFinal.i  => ResponseFinal.asRight
    case ResponseChunk.i  => ResponseChunk.asRight
    case Authentication.i => Authentication.asRight
    case i                => VPackError.IllegalValue(s"unknown message type $i").asLeft
  }
}
