package avokka.arangodb
package api

import avokka.velocypack._

object Document {

  case class Response[T](
      _id: DocumentHandle,
      _key: DocumentKey,
      _rev: String,
      `new`: Option[T] = None,
      old: Option[T] = None,
      _oldRev: Option[String] = None,
  )

  object Response {
    implicit def decoder[T: VPackDecoder]: VPackDecoder[Response[T]] =
      VPackRecord[Response[T]].decoderWithDefaults
  }

  val filterEmptyInternalAttributes: ((String, VPack)) => Boolean = {
    case (DocumentHandle.key, value) if value.isEmpty   => false
    case (DocumentKey.key, value) if value.isEmpty      => false
    case (DocumentRevision.key, value) if value.isEmpty => false
    case _                                              => true
  }

}