package avokka.arangodb.models

import avokka.velocypack._

case class GraphEdge[T](
  edge: T,
)

object GraphEdge {
  implicit def decoder[T: VPackDecoder]: VPackDecoder[GraphEdge[T]] = VPackDecoder.derived
}
