package avokka.arangodb

import avokka.velocypack._
import avokka.velocypack.codecs.VPackRecordCodec
import scodec.Decoder

case class AuthResponse
(
  code: Long,
  error: Boolean,
  errorMessage: String,
  errorNum: Long
)

object AuthResponse {
  implicit val decoder: Decoder[AuthResponse] = VPackRecordCodec[AuthResponse].codec
}
