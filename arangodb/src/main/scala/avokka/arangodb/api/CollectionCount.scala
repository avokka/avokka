package avokka.arangodb
package api

import avokka.velocypack._

final case class CollectionCount(
    count: Long,
)

object CollectionCount {

  implicit val decoder: VPackDecoder[CollectionCount] = VPackDecoder.gen

}
