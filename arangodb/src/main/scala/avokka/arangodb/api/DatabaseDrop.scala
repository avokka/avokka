package avokka.arangodb
package api

import avokka.velocypack._

case class DatabaseDrop(
    result: Boolean
)

object DatabaseDrop {
  implicit val decoder: VPackDecoder[DatabaseDrop] = VPackDecoder.gen
}
