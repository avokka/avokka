package avokka.arangodb

import avokka.velocypack
import cats.implicits._
import scodec.interop.cats._
import scodec.{Attempt, Codec, Err}

sealed abstract class CollectionType(val i: Long)

object CollectionType {

  case object Unknown extends CollectionType(0)
  case object Document extends CollectionType(2)
  case object Edge extends CollectionType(3)

  implicit val codec: Codec[CollectionType] = velocypack.longCodec.exmap({
    case Unknown.i => Unknown.pure[Attempt]
    case Document.i => Document.pure[Attempt]
    case Edge.i => Edge.pure[Attempt]
    case i => Err(s"unknown collection type $i").raiseError
  }, _.i.pure[Attempt])
}

