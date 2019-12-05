package avokka.arangodb

import avokka.velocypack._
import avokka.velocypack.codecs.{VPackRecordCodec, VPackRecordDefaultsCodec}
import scodec.Codec

case class ApiVersion
(
  server: String,
  license: String,
  version: String,
  details: Map[String, String] = Map.empty
)

object ApiVersion {
  implicit val VersionResponseCodec: Codec[ApiVersion] = VPackRecordDefaultsCodec.deriveFor[ApiVersion]().codec
}
