package avokka.arangodb

import avokka.velocypack.{VPackDecoder, VPackEncoder, VPackError}
import cats.syntax.either._

sealed abstract class CollectionStatus(val i: Int)

object CollectionStatus {

  case object Unknown extends CollectionStatus(0)
  case object NewBorn extends CollectionStatus(1)
  case object Unloaded extends CollectionStatus(2)
  case object Loaded extends CollectionStatus(3)
  case object Unloading extends CollectionStatus(4)
  case object Deleted extends CollectionStatus(5)
  case object Loading extends CollectionStatus(6)

  implicit val encoder: VPackEncoder[CollectionStatus] = VPackEncoder[Int].contramap(_.i)
  implicit val decoder: VPackDecoder[CollectionStatus] = VPackDecoder[Int].emap {
    case Unknown.i => Unknown.asRight
    case NewBorn.i => NewBorn.asRight
    case Unloaded.i => Unloaded.asRight
    case Loaded.i => Loaded.asRight
    case Unloading.i => Unloading.asRight
    case Deleted.i => Deleted.asRight
    case Loading.i => Loading.asRight
    case i => VPackError.IllegalValue(s"unknown collection status $i").asLeft
  }
}

