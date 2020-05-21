package avokka.arangodb

import avokka.velocypack._

final case class ArangoResponse[T](
    header: ArangoResponse.Header,
    body: T
)

object ArangoResponse {

  final case class Header(
      version: Int,
      `type`: MessageType,
      responseCode: Int,
      meta: Map[String, String] = Map.empty
  )

  object Header {
    implicit val decoder: VPackDecoder[Header] = VPackGeneric[Header].decoder
  }

}
