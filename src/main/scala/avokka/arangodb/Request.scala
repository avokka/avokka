package avokka.arangodb

import avokka.velocypack.VPackEncoder
import scodec.Encoder

sealed trait Request {
  def header: RequestHeader
}

object Request {

  case class Head
  (
    header: RequestHeader,
  ) extends Request

  object Head {
    implicit val encoder: Encoder[Head] = RequestHeader.encoder.serializer.contramap(_.header)
  }

  case class Payload[T]
  (
    header: RequestHeader,
    body: T
  ) extends Request

  object Payload {
    implicit def encoder[T](implicit bodyEncoder: VPackEncoder[T]): Encoder[Payload[T]] = Encoder { request =>
      Encoder.encodeBoth(RequestHeader.encoder.serializer, bodyEncoder.serializer)(request.header, request.body)
    }
  }

}