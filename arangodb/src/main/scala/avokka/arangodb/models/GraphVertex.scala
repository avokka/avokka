package avokka.arangodb.models

import avokka.velocypack._

case class GraphVertex[T](
  vertex: T,
)

object GraphVertex {
  implicit def decoder[T: VPackDecoder]: VPackDecoder[GraphVertex[T]] = VPackDecoder.gen
}
