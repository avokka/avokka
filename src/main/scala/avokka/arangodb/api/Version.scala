package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class Version
(
  server: String,
  license: String,
  version: String,
  details: Map[String, String] = Map.empty
)

object Version {
  implicit val codec: Codec[Version] = VPackRecord[Version].codecWithDefaults
}
