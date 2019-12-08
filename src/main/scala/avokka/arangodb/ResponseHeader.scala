package avokka.arangodb

import avokka.velocypack._
import scodec.Codec

case class ResponseHeader
(
  version: Int,
  `type`: MessageType,
  responseCode: Int,
  meta: Map[String, String] = Map.empty
)

object ResponseHeader {
  implicit val codec: Codec[ResponseHeader] = VPackGeneric[ResponseHeader].codec(true)
}