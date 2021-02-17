package avokka.arangodb
package api

import avokka.velocypack._
import types._

case class Document[T](
                        _id: DocumentHandle,
                        _key: DocumentKey,
                        _rev: DocumentRevision,
                        `new`: Option[T] = None,
                        old: Option[T] = None,
                        _oldRev: Option[DocumentRevision] = None,
                      )

object Document {

  implicit def decoder[T: VPackDecoder]: VPackDecoder[Document[T]] = VPackDecoder.gen

  val filterEmptyInternalAttributes: ((String, VPack)) => Boolean = {
    case (DocumentHandle.key, value) if value.isEmpty   => false
    case (DocumentKey.key, value) if value.isEmpty      => false
    case (DocumentRevision.key, value) if value.isEmpty => false
    case _                                              => true
  }

}
