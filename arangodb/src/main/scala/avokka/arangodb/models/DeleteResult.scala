package avokka.arangodb
package models

import avokka.velocypack._

final case class DeleteResult(
    id: String
)

object DeleteResult {

  implicit val decoder: VPackDecoder[DeleteResult] = VPackDecoder.gen

}
