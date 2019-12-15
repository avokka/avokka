package avokka.arangodb

import avokka.velocypack.{VPackEncoder, codecs}
import scodec.Encoder

case class Request[T]
(
  header: RequestHeader,
  body: T
)

object Request {

  def serializer[T](e: VPackEncoder[T]): Encoder[T] = codecs.vpackEncoder.contramap(e.encode)

  implicit val unitEncoder: Encoder[Request[Unit]] = serializer(RequestHeader.encoder).contramap(_.header)
  implicit def encoder[T](implicit bodyEncoder: VPackEncoder[T]): Encoder[Request[T]] = Encoder { request =>
    Encoder.encodeBoth(serializer(RequestHeader.encoder), serializer(bodyEncoder))(request.header, request.body)
  }

}