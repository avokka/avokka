package avokka.arangodb

import avokka.velocypack._
import avokka.velocypack.codecs.VPackRecordCodec
import scodec.Codec

case class ApiVersion
(
  server: String,
  license: String,
  version: String
)

object ApiVersion {
  implicit val VersionResponseCodec: Codec[ApiVersion] = VPackRecordCodec.deriveFor[ApiVersion].codec
}
