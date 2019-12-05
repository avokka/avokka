package avokka.arangodb

import avokka.velocypack._
import avokka.velocypack.codecs.VPackHListCodec
import scodec.Codec
import shapeless.{::, HNil}

case class RequestHeader
(
  version: Int = 1,
  `type`: MessageType = MessageType.Request,
  database: String,
  requestType: RequestType,
  request: String,
  parameters: Map[String, String] = Map.empty,
  meta: Map[String, String] = Map.empty,
)

object RequestHeader {

  implicit val codec: Codec[RequestHeader] = VPackHListCodec.codecCompact[
    Int ::
    MessageType ::
    String ::
    RequestType ::
    String ::
    Map[String, String] ::
    Map[String, String] ::
    HNil
  ].as

}
