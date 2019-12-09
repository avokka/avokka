package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class DatabaseInfo
(
  name: String,
  id: String,
  path: String,
  isSystem: Boolean,
)

object DatabaseInfo {
  implicit val codec: Codec[DatabaseInfo] = VPackRecord[DatabaseInfo].codec
}
