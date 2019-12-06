package avokka.arangodb

import avokka.velocypack._
import avokka.velocypack.codecs.VPackHListCodec
import scodec.Encoder
import shapeless.{::, HNil}

case class RequestAuthentication
(
  version: Int = 1,
  `type`: MessageType = MessageType.Authentication,
  encryption: String,
  user: String,
  password: String
)

object RequestAuthentication {

  implicit val encoder: Encoder[RequestAuthentication] = VPackHListCodec.encoderCompact[
    Int ::
    MessageType ::
    String ::
    String ::
    String ::
    HNil
  ].as

}