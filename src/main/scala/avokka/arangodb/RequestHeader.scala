package avokka.arangodb

import avokka.velocypack._

case class RequestHeader
(
  version: Int = 1,
  `type`: MessageType = MessageType.Request,
  database: DatabaseName,
  requestType: RequestType,
  request: String,
  parameters: Map[String, String] = Map.empty,
  meta: Map[String, String] = Map.empty,
)

object RequestHeader {
  implicit val encoder: VPackEncoder[RequestHeader] = VPackGeneric[RequestHeader].encoder
  implicit val decoder: VPackDecoder[RequestHeader] = VPackGeneric[RequestHeader].decoder
}
