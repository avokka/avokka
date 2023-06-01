package avokka.arangodb.types

import avokka.velocypack._

case class CollectionName(repr: String) {
  def isEmpty: Boolean = repr.isEmpty
}

object CollectionName {
  implicit val encoder: VPackEncoder[CollectionName] = VPackEncoder.stringEncoder.contramap(_.repr)
  implicit val decoder: VPackDecoder[CollectionName] = VPackDecoder.stringDecoder.map(apply)
}
