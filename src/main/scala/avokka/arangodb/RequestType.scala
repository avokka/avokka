package avokka.arangodb

import avokka.velocypack
import cats.implicits._
import scodec.interop.cats._
import scodec.{Attempt, Codec, Err}

sealed abstract class RequestType(val i: Long)

object RequestType {

  case object DELETE extends RequestType(0)
  case object GET extends RequestType(1)
  case object POST extends RequestType(2)
  case object PUT extends RequestType(3)
  case object HEAD extends RequestType(4)
  case object PATCH extends RequestType(5)
  case object OPTIONS extends RequestType(6)

  implicit val codec: Codec[RequestType] = velocypack.longCodec.exmap({
    case DELETE.i => DELETE.pure[Attempt]
    case GET.i => GET.pure[Attempt]
    case POST.i => POST.pure[Attempt]
    case PUT.i => PUT.pure[Attempt]
    case HEAD.i => HEAD.pure[Attempt]
    case PATCH.i => PATCH.pure[Attempt]
    case OPTIONS.i => OPTIONS.pure[Attempt]
    case i => Err(s"unknown request type $i").raiseError
  }, _.i.pure[Attempt])
}
