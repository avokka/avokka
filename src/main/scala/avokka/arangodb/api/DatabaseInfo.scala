package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack._
import scodec.Codec

case class DatabaseInfo
(
  name: DatabaseName,
  id: String,
  path: String,
  isSystem: Boolean,
)

object DatabaseInfo {
  implicit val decoder: VPackDecoder[DatabaseInfo] = VPackRecord[DatabaseInfo].decoderWithDefaults
}
