package avokka

import avokka.velocypack.codecs.VPackHListCodec
import scodec.Codec
import shapeless.{HNil, ::}
import avokka.velocypack._

case class VAuthRequest
(
  version: Int,
  `type`: Int,
  encryption: String,
  user: String,
  password: String
)

object VAuthRequest {
  val codec: Codec[VAuthRequest] = VPackHListCodec.codecCompact[
    Int ::
    Int ::
    String ::
    String ::
    String ::
    HNil
  ].as
}