package avokka.arangodb
package api

import avokka.velocypack._

final case class IndexDelete(
    id: String
)

object IndexDelete {

  implicit val decoder: VPackDecoder[IndexDelete] = VPackDecoder.gen

}
