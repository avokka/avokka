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
    implicit def decoder[T: VPackDecoder]: VPackDecoder[Response[T]] = VPackRecord[Response[T]].decoderWithDefaults
  }

}
