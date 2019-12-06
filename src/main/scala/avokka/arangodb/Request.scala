package avokka.arangodb

import scodec.Encoder

case class Request[T]
(
  header: RequestHeader,
  body: T
)

object Request {

  implicit def encoder[T](implicit bodyEncoder: Encoder[T]): Encoder[Request[T]] = Encoder { request =>
    Encoder.encodeBoth(RequestHeader.codec, bodyEncoder)(request.header, request.body)
  }

}