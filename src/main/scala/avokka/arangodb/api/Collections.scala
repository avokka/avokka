package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class Collections
(
  result: Vector[Collection]
)

object Collections {
  implicit val codec: Codec[Collections] = VPackRecord[Collections].codec
}


