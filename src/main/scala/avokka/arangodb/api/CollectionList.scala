package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

case class CollectionList
(
  result: Vector[CollectionInfo]
)

object CollectionList {
  implicit val codec: Codec[CollectionList] = VPackRecord[CollectionList].codec
}


