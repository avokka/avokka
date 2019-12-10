package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack._
import scodec.Codec

case class DocumentCreate[T]
(
  _id: DocumentHandle,
  _key: DocumentKey,
  _rev: String,
  `new`: Option[T] = None,
  old: Option[T] = None,
)

object DocumentCreate {
  implicit def codec[T: Codec]: Codec[DocumentCreate[T]] = VPackRecord[DocumentCreate[T]].codecWithDefaults

}
