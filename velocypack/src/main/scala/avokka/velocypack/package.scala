package avokka

import cats.implicits._
import scodec.bits.{BitVector, ByteVector}
import scodec.{DecodeResult, Decoder, Encoder}

package object velocypack extends CodecImplicits {

  implicit class SyntaxToVPack[T](v: T) {
    def toVPack(implicit encoder: Encoder[T]): Either[VPackError, ByteVector] = encoder.encode(v).fold(
      e => VPackError.Codec(e).asLeft, _.bytes.asRight
    )
  }

  implicit class SyntaxFromVPack(bits: BitVector) {
    def fromVPack[T](implicit decoder: Decoder[T]): Either[VPackError, DecodeResult[T]] = decoder.decode(bits).fold(
      e => VPackError.Codec(e).asLeft, _.asRight
    )
  }

}
