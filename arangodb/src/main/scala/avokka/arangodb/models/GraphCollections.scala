package avokka.arangodb
package models

import avokka.velocypack._
import types.CollectionName

/**
  * @param collections list of collections
  */
final case class GraphCollections(
  collections: Vector[CollectionName]
)

object GraphCollections {
  implicit val decoder: VPackDecoder[GraphCollections] = VPackDecoder.derived
}