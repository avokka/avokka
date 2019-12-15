package avokka.arangodb

import avokka.velocypack._

case class ResponseHeader
(
  version: Int,
  `type`: MessageType,
  responseCode: Int,
  meta: Map[String, String] = Map.empty
)

object ResponseHeader {
//  implicit val encoder: VPackEncoder[ResponseHeader] = VPackGeneric[ResponseHeader].encoder
  implicit val decoder: VPackDecoder[ResponseHeader] = VPackGeneric[ResponseHeader].decoder
}