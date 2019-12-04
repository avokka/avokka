package avokka.velocystream

import scodec.Encoder

case class VRequest[T]
(
  header: VRequestHeader,
  body: T
)

object VRequest {
  implicit def encoder[T](implicit tEncoder: Encoder[T]): Encoder[VRequest[T]] = Encoder { request =>
    Encoder.encodeBoth(VRequestHeader.codec, tEncoder)(request.header, request.body)
  }
}