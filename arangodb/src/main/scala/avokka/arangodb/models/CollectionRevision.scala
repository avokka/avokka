package avokka.arangodb
package models

import avokka.velocypack._

final case class CollectionRevision(
    revision: String,
)

object CollectionRevision {

  implicit val decoder: VPackDecoder[CollectionRevision] = VPackDecoder.gen

}
