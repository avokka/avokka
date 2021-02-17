package avokka.arangodb
package api

import avokka.velocypack._
import types._

final case class DatabaseList(
    result: Vector[DatabaseName]
)

object DatabaseList {
  implicit val decoder: VPackDecoder[DatabaseList] = VPackDecoder.gen
}
