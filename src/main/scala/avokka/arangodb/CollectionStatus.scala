package avokka.arangodb

import avokka.velocypack
import cats.implicits._
import scodec.interop.cats._
import scodec.{Attempt, Codec, Err}

sealed abstract class CollectionStatus(val i: Long)

object CollectionStatus {

  case object Unknown extends CollectionStatus(0)
  case object NewBorn extends CollectionStatus(1)
  case object Unloaded extends CollectionStatus(2)
  case object Loaded extends CollectionStatus(3)
  case object Unloading extends CollectionStatus(4)
  case object Deleted extends CollectionStatus(5)
  case object Loading extends CollectionStatus(6)

  implicit val codec: Codec[CollectionStatus] = velocypack.longCodec.exmap({
    case Unknown.i => Unknown.pure[Attempt]
    case NewBorn.i => NewBorn.pure[Attempt]
    case Unloaded.i => Unloaded.pure[Attempt]
    case Loaded.i => Loaded.pure[Attempt]
    case Unloading.i => Unloading.pure[Attempt]
    case Deleted.i => Deleted.pure[Attempt]
    case Loading.i => Loading.pure[Attempt]
    case i => Err(s"unknown collection status $i").raiseError
  }, _.i.pure[Attempt])
}

