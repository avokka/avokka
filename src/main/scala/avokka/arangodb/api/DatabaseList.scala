package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack._
import scodec.Codec

case class DatabaseList
(
  result: Vector[DatabaseName]
)

object DatabaseList {
  implicit val codec: Codec[DatabaseList] = VPackRecord[DatabaseList].codec
}