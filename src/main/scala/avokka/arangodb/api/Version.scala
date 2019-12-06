package avokka.arangodb.api

import avokka.velocypack._
import avokka.velocypack.codecs.VPackRecordDefaultsCodec
import scodec.Codec

case class Version
(
  server: String,
  license: String,
  version: String,
  details: Map[String, String] = Map.empty
)

object Version {
  implicit val VersionResponseCodec: Codec[Version] = VPackRecordDefaultsCodec[Version].codec
}