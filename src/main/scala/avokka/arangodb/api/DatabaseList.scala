package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack._
import scodec.Codec

case class DatabaseList
(
  result: Vector[DatabaseName]
)

object DatabaseList {
  implicit val decoder: VPackDecoder[DatabaseList] = VPackRecord[DatabaseList].decoderWithDefaults
}
