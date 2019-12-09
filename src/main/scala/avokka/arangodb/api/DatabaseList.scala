package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class DatabaseList
(
  result: Vector[String]
)

object DatabaseList {
  implicit val codec: Codec[DatabaseList] = VPackRecord[DatabaseList].codec
}
