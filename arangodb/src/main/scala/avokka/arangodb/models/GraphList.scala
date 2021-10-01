package avokka.arangodb
package models

import avokka.velocypack._

/**
  * @param graphs list of graph representations
  */
final case class GraphList(
  graphs: Vector[GraphInfo.GraphRepresentation]
)

object GraphList {
  implicit val decoder: VPackDecoder[GraphList] = VPackDecoder.gen
}
