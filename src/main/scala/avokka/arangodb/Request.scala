package avokka.arangodb

import avokka.velocypack.VPackEncoder
import scodec.Encoder

case class Request[T]
(
  header: RequestHeader,
  body: T
)

object Request {

  implicit val unitEncoder: Encoder[Request[Unit]] = RequestHeader.encoder.serializer.contramap(_.header)
  implicit def encoder[T](implicit bodyEncoder: VPackEncoder[T]): Encoder[Request[T]] = Encoder { request =>
    Encoder.encodeBoth(RequestHeader.encoder.serializer, bodyEncoder.serializer)(request.header, request.body)
  }

}