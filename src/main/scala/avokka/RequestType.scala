package avokka

import cats.implicits._
import scodec.{Attempt, Codec, Err}
import scodec.interop.cats._

sealed trait RequestType {
  def i: Long
}

object RequestType {
  abstract class RequestTypeAbstract(val i: Long) extends RequestType

  case object DELETE extends RequestTypeAbstract(0)
  case object GET extends RequestTypeAbstract(1)
  case object POST extends RequestTypeAbstract(2)
  case object PUT extends RequestTypeAbstract(3)
  case object HEAD extends RequestTypeAbstract(4)
  case object PATCH extends RequestTypeAbstract(5)
  case object OPTIONS extends RequestTypeAbstract(6)

  implicit val codec: Codec[RequestType] = velocypack.longCodec.exmap({
    case DELETE.i => DELETE.pure[Attempt]
    case GET.i => GET.pure[Attempt]
    case POST.i => POST.pure[Attempt]
    case PUT.i => PUT.pure[Attempt]
    case HEAD.i => Err("request type HEAD is not used in VPP").raiseError
    case PATCH.i => PATCH.pure[Attempt]
    case OPTIONS.i => Err("request type OPTIONS is not used in VPP").raiseError
    case i => Err(s"unknown request type $i").raiseError
  }, _.i.pure[Attempt])
}
