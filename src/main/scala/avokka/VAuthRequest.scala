package avokka

import avokka.velocypack.codecs.VPackHListCodec
import scodec.Codec
import shapeless.HNil

case class VAuthRequest
(
  version: Int,
  `type`: Int,
  encryption: String,
  user: String,
  password: String
)

object VAuthRequest {
  val codec: Codec[VAuthRequest] = VPackHListCodec.codecCompact(
    velocypack.intCodec ::
    velocypack.intCodec ::
    velocypack.stringCodec ::
    velocypack.stringCodec ::
    velocypack.stringCodec ::
    HNil
  ).as
}