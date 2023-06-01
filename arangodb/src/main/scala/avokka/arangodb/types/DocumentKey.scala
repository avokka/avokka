package avokka.arangodb.types

import avokka.velocypack._

case class DocumentKey(repr: String) {
  def isEmpty: Boolean = repr.isEmpty
}

object DocumentKey {
  val key: String = "_key"
  implicit val encoder: VPackEncoder[DocumentKey] = VPackEncoder.stringEncoder.contramap(_.repr)
  implicit val decoder: VPackDecoder[DocumentKey] = VPackDecoder.stringDecoder.map(apply)
  val empty = apply("")
}
