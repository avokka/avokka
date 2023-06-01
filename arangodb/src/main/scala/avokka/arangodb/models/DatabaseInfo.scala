package avokka.arangodb
package models

import avokka.velocypack._
import types._

/** database information
  *
  * @param name database name
  * @param id database id
  * @param path filesystem path
  * @param isSystem whether or not the current database is the _system database
  */
case class DatabaseInfo(
    name: DatabaseName,
    id: String,
    path: String,
    isSystem: Boolean,
)

object DatabaseInfo {
  implicit val decoder: VPackDecoder[DatabaseInfo] = VPackDecoder.derived
}
