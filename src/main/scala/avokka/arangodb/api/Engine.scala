package avokka.arangodb.api

import avokka.velocypack._
import scodec.Decoder

case class Engine
(
  name: String,
)

object Engine {
  implicit val decoder: Decoder[Engine] = VPackRecord[Engine].codecWithDefaults
}