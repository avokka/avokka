package avokka.arangodb.models

import avokka.velocypack._

final case class CollectionSchema(
    rule: VPack,
    level: String,
    message: String
)

object CollectionSchema {
  implicit val encoder: VPackEncoder[CollectionSchema] = VPackEncoder.derived
  implicit val decoder: VPackDecoder[CollectionSchema] = VPackDecoder.derived
}
