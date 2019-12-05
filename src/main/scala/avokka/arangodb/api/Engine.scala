package avokka.arangodb.api

import avokka.velocypack._
import avokka.velocypack.codecs.VPackRecordDefaultsCodec
import scodec.Decoder

case class Engine
(
  name: String,
)

object Engine {
  implicit val decoder: Decoder[Engine] = VPackRecordDefaultsCodec[Engine].codec
}