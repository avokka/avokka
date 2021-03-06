package avokka.arangodb
package api

import avokka.velocypack._
import types._

case class DatabaseInfo(
    name: DatabaseName,
    id: String,
    path: String,
    isSystem: Boolean,
)

object DatabaseInfo {

  implicit val decoder: VPackDecoder[DatabaseInfo] = VPackDecoder.gen

}
