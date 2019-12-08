package avokka.arangodb

import avokka.velocypack
import cats.implicits._
import scodec.interop.cats._
import scodec.{Attempt, Codec, Err}

sealed trait CollectionType {
  def i: Long
}

object CollectionType {
  abstract class CollectionTypeAbstrat(val i: Long) extends CollectionType

  case object Unknown extends CollectionTypeAbstrat(0)
  case object Document extends CollectionTypeAbstrat(2)
  case object Edge extends CollectionTypeAbstrat(3)

  implicit val codec: Codec[CollectionType] = velocypack.longCodec.exmap({
    case Unknown.i => Unknown.pure[Attempt]
    case Document.i => Document.pure[Attempt]
    case Edge.i => Edge.pure[Attempt]
    case i => Err(s"unknown collection type $i").raiseError
  }, _.i.pure[Attempt])
}

