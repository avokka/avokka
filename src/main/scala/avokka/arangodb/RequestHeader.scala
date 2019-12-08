package avokka.arangodb

import avokka.velocypack._
import scodec.Codec

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
  implicit val codec: Codec[RequestHeader] = VPackGeneric[RequestHeader].codec(true)
}
