package avokka.arangodb
package models

import types.CollectionName
import avokka.velocypack._

/** A definition for the relations of the graph
  *
  * @param collection Name of the edge collection, where the edge are stored in.
  * @param from List of vertex collection names. Edges in collection can only be inserted if their _from is in any of the collections here.
  * @param to List of vertex collection names. Edges in collection can only be inserted if their _to is in any of the collections here.
  */
final case class GraphEdgeDefinition(
  collection: CollectionName,
  from: List[CollectionName],
  to: List[CollectionName]
)
object GraphEdgeDefinition {
  implicit val encoder: VPackEncoder[GraphEdgeDefinition] = VPackEncoder.gen
  implicit val decoder: VPackDecoder[GraphEdgeDefinition] = VPackDecoder.gen
}