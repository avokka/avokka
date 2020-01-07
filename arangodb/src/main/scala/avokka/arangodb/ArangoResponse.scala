package avokka.arangodb

import avokka.velocypack._

case class ArangoResponse[T](
    header: ArangoResponse.Header,
    body: T
)

object ArangoResponse {

  case class Header(
      version: Int,
      `type`: MessageType,
      responseCode: Int,
      meta: Map[String, String] = Map.empty
  )

  object Header {
    implicit val decoder: VPackDecoder[Header] = VPackGeneric[Header].decoder
  }

}
