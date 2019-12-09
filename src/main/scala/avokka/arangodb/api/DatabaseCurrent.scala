package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class DatabaseCurrent
(
  result: DatabaseInfo
)

object DatabaseCurrent {
  implicit val codec: Codec[DatabaseCurrent] = VPackRecord[DatabaseCurrent].codec

}
