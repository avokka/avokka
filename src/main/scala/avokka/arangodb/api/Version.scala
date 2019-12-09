package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

/**
 *
 * @param server  will always contain arango
 * @param license the server license
 * @param version the server version string. The string has the format
 *                "major.minor.sub". major and minor will be numeric, and sub
 *                may contain a number or a textual version
 * @param details additional information about included components and their versions
 */
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
