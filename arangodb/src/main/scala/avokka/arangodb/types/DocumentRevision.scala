package avokka.arangodb.types

import avokka.velocypack._

case class DocumentRevision(repr: String)

object DocumentRevision {
  val key: String = "_rev"
  implicit val encoder: VPackEncoder[DocumentRevision] = VPackEncoder[String].contramap(_.repr)
  implicit val decoder: VPackDecoder[DocumentRevision] = VPackDecoder[String].map(apply)
  val empty = apply("")
}
