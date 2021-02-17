package avokka.arangodb
package api

import avokka.velocypack._

final case class CollectionDrop(
    id: String
)

object CollectionDrop {

  implicit val decoder: VPackDecoder[CollectionDrop] = VPackDecoder.gen

}
