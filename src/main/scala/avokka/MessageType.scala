package avokka

import cats.implicits._
import scodec.{Attempt, Codec, Err}
import scodec.interop.cats._

sealed trait MessageType {
  def i: Long
}

object MessageType {
  abstract class MessageTypeAbstrat(val i: Long) extends MessageType

  case object Request extends MessageTypeAbstrat(1)
  case object ResponseFinal extends MessageTypeAbstrat(2)
  case object ResponseChunk extends MessageTypeAbstrat(3)
  case object Authentication extends MessageTypeAbstrat(1000)

  implicit val codec: Codec[MessageType] = velocypack.longCodec.exmap({
    case Request.i => Request.pure[Attempt]
    case ResponseFinal.i => ResponseFinal.pure[Attempt]
    case ResponseChunk.i => ResponseChunk.pure[Attempt]
    case Authentication.i => Authentication.pure[Attempt]
    case i => Err(s"unknown message type $i").raiseError
  }, _.i.pure[Attempt])
}
