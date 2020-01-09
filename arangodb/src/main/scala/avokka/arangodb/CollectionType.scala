package avokka.arangodb

import avokka.velocypack.{VPackDecoder, VPackEncoder, VPackError}
import cats.syntax.either._

sealed abstract class CollectionType(val i: Int)

object CollectionType {

  case object Unknown extends CollectionType(0)
  case object Document extends CollectionType(2)
  case object Edge extends CollectionType(3)

  implicit val encoder: VPackEncoder[CollectionType] = VPackEncoder[Int].contramap(_.i)
  implicit val decoder: VPackDecoder[CollectionType] = VPackDecoder[Int].emap {
    case Unknown.i  => Unknown.asRight
    case Document.i => Document.asRight
    case Edge.i     => Edge.asRight
    case i          => VPackError.IllegalValue(s"unknown collection type $i").asLeft
  }
}