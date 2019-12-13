package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack._

case class DocumentCreate[T]
(
  _id: DocumentHandle,
  _key: DocumentKey,
  _rev: String,
  `new`: Option[T] = None,
  old: Option[T] = None,
)

object DocumentCreate {
  implicit def decoder[T](implicit d: VPackDecoder[T]): VPackDecoder[DocumentCreate[T]] =
    VPackRecord[DocumentCreate[T]].decoderWithDefaults

}
