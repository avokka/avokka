package avokka.arangodb.types

import avokka.velocypack._

case class TransactionId(repr: String)

object TransactionId {
  implicit val encoder: VPackEncoder[TransactionId] = VPackEncoder[String].contramap(_.repr)
  implicit val decoder: VPackDecoder[TransactionId] = VPackDecoder[String].map(apply)
}
