package avokka.arangodb
package api

import avokka.velocypack._

final case class DatabaseResult(
    result: Boolean
)

object DatabaseResult {
  implicit val decoder: VPackDecoder[DatabaseResult] = VPackDecoder.gen
}
