package avokka.arangodb.models

import avokka.velocypack.{VPackDecoder, VPackEncoder, VPackError}

sealed abstract class CollectionType(val i: Int) extends Product with Serializable

object CollectionType {

  case object Unknown extends CollectionType(0)
  case object Document extends CollectionType(2)
  case object Edge extends CollectionType(3)

  implicit val encoder: VPackEncoder[CollectionType] = VPackEncoder[Int].contramap(_.i)
  implicit val decoder: VPackDecoder[CollectionType] = VPackDecoder[Int].flatMap {
    case Unknown.i  => Right(Unknown)
    case Document.i => Right(Document)
    case Edge.i     => Right(Edge)
    case i          => Left(VPackError.IllegalValue(s"unknown collection type $i"))
  }
}
