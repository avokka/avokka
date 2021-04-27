package avokka.arangodb.models

import avokka.velocypack._

final case class CollectionSchema(
    rule: VPack,
    level: String,
    message: String
)

object CollectionSchema {
  implicit val encoder: VPackEncoder[CollectionSchema] = VPackEncoder.gen
  implicit val decoder: VPackDecoder[CollectionSchema] = VPackDecoder.gen
}
