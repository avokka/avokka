package avokka.velocystream

import avokka.velocypack._
import avokka.velocypack.codecs.VPackRecordCodec
import scodec.{Codec, Decoder}

case class VAuthResponse
(
  code: Long,
  error: Boolean,
  errorMessage: String,
  errorNum: Long
)

object VAuthResponse {
  implicit val decoder: Decoder[VAuthResponse] = VPackRecordCodec.deriveFor[VAuthResponse].codec
}
