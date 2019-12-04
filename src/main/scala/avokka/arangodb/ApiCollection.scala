package avokka.arangodb

import avokka.velocypack._
import avokka.velocypack.codecs.VPackRecordCodec
import scodec.Codec

case class ApiCollection
(
  error: Boolean,
  code: Long,
  result: VPackArray
)

object ApiCollection {
  implicit val codec: Codec[ApiCollection] = VPackRecordCodec.deriveFor[ApiCollection].codec
}


