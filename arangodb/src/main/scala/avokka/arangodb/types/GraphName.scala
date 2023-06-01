package avokka.arangodb.types

import avokka.velocypack._

case class GraphName(repr: String)

object GraphName {
  implicit val encoder: VPackEncoder[GraphName] = VPackEncoder[String].contramap(_.repr)
  implicit val decoder: VPackDecoder[GraphName] = VPackDecoder[String].map(apply)
}
