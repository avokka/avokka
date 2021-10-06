package avokka.arangodb
package models

import avokka.velocypack._

final case class RemovedResult(
  removed: Boolean
)

object RemovedResult {
  implicit val decoder: VPackDecoder[RemovedResult] = VPackDecoder.gen
}
