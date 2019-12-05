package avokka.arangodb

import scodec.Encoder

case class Request[T]
(
  header: RequestHeader,
  body: T
)

object Request {

  implicit def encoder[T](implicit tEncoder: Encoder[T]): Encoder[Request[T]] = Encoder { request =>
    Encoder.encodeBoth(RequestHeader.codec, tEncoder)(request.header, request.body)
  }

}