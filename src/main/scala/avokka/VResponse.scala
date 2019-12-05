package avokka

import scodec.Decoder

case class VResponse[T]
(
  header: VResponseHeader,
  body: T
)

object VResponse {

  implicit def decoder[T](implicit tDecoder: Decoder[T]): Decoder[VResponse[T]] = Decoder { bits =>
    Decoder.decodeBothCombine(VResponseHeader.codec, tDecoder)(bits)(VResponse[T])
  }

}
