package avokka.arangodb

import scodec.Decoder

case class Response[T]
(
  header: ResponseHeader,
  body: T
)

object Response {

  implicit def decoder[T](implicit tDecoder: Decoder[T]): Decoder[Response[T]] = Decoder { bits =>
    Decoder.decodeBothCombine(ResponseHeader.codec, tDecoder)(bits)(Response[T])
  }

}
