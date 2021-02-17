package avokka.arangodb
package api

import avokka.velocypack._

final case class CollectionList(
    result: Vector[CollectionInfo]
)

object CollectionList {

  implicit val decoder: VPackDecoder[CollectionList] = VPackDecoder.gen

}
