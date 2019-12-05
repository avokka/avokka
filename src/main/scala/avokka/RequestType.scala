package avokka

import cats.implicits._
import scodec.{Attempt, Codec}
import scodec.interop.cats._

sealed trait RequestType {
  def i: Long
}

object RequestType {
  abstract class RequestTypeAbstract(val i: Long) extends RequestType

  case object DELETE extends RequestTypeAbstract(0)
  case object GET extends RequestTypeAbstract(1)

  implicit val codec: Codec[RequestType] = velocypack.longCodec.exmap({
    case DELETE.i => DELETE.pure[Attempt]
    case GET.i => GET.pure[Attempt]
  }, _.i.pure[Attempt])
}
