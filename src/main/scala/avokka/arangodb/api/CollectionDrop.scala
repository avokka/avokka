package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class CollectionDrop
(
  id: String
)

object CollectionDrop {
  implicit val codec: Codec[CollectionDrop] = VPackRecord[CollectionDrop].codec
}


