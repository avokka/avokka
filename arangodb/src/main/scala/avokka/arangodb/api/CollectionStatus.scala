package avokka.arangodb.api

import avokka.velocypack.{VPackDecoder, VPackEncoder, VPackError}

sealed abstract class CollectionStatus(val i: Int) extends Product with Serializable

object CollectionStatus {

  case object Unknown extends CollectionStatus(0)
  case object NewBorn extends CollectionStatus(1)
  case object Unloaded extends CollectionStatus(2)
  case object Loaded extends CollectionStatus(3)
  case object Unloading extends CollectionStatus(4)
  case object Deleted extends CollectionStatus(5)
  case object Loading extends CollectionStatus(6)

  implicit val encoder: VPackEncoder[CollectionStatus] = VPackEncoder[Int].contramap(_.i)
  implicit val decoder: VPackDecoder[CollectionStatus] = VPackDecoder[Int].flatMap {
    case Unknown.i   => Right(Unknown)
    case NewBorn.i   => Right(NewBorn)
    case Unloaded.i  => Right(Unloaded)
    case Loaded.i    => Right(Loaded)
    case Unloading.i => Right(Unloading)
    case Deleted.i   => Right(Deleted)
    case Loading.i   => Right(Loading)
    case i           => Left(VPackError.IllegalValue(s"unknown collection status $i"))
  }
}
