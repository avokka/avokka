package avokka.arangodb
package api

import avokka.velocypack._

final case class IndexList(
    indexes: List[Index],
    identifiers: Map[String, Index]
)

object IndexList {

  implicit val decoder: VPackDecoder[IndexList] = VPackDecoder.gen

}
