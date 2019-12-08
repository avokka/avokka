package avokka.arangodb

import avokka.velocypack
import cats.implicits._
import scodec.interop.cats._
import scodec.{Attempt, Codec, Err}

sealed abstract class MessageType(val i: Long)

object MessageType {

  case object Request extends MessageType(1)
  case object ResponseFinal extends MessageType(2)
  case object ResponseChunk extends MessageType(3)
  case object Authentication extends MessageType(1000)

  implicit val codec: Codec[MessageType] = velocypack.longCodec.exmap({
    case Request.i => Request.pure[Attempt]
    case ResponseFinal.i => ResponseFinal.pure[Attempt]
    case ResponseChunk.i => ResponseChunk.pure[Attempt]
    case Authentication.i => Authentication.pure[Attempt]
    case i => Err(s"unknown message type $i").raiseError
  }, _.i.pure[Attempt])
}
