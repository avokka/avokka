package avokka.arangodb.api

import avokka.velocypack._
import avokka.velocypack.codecs.VPackRecordCodec
import scodec.Codec

case class Database
(
  error: Boolean,
  code: Long,
  result: Vector[String]
)

object Database {
  implicit val codec: Codec[Database] = VPackRecordCodec[Database].codec
}
