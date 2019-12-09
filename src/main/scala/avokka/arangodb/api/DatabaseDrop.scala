package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class DatabaseDrop
(
  result: Boolean
)

object DatabaseDrop {
  implicit val codec: Codec[DatabaseDrop] = VPackRecord[DatabaseDrop].codecWithDefaults
}
