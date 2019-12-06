package avokka

import cats.data.Validated
import cats.implicits._
import scodec.bits.{BitVector, ByteVector}
import scodec.{DecodeResult, Decoder, Encoder}

package object velocypack extends CodecImplicits {

  implicit class SyntaxToVPack[T](v: T) {
    def toVPack(implicit encoder: Encoder[T]): Validated[VPackError, ByteVector] = encoder.encode(v).fold(
      e => VPackError.Codec(e.toString).invalid, _.bytes.valid
    )
  }

  implicit class SyntaxFromVPack(bits: BitVector) {
    def fromVPack[T](implicit decoder: Decoder[T]): Validated[VPackError, DecodeResult[T]] = decoder.decode(bits).fold(
      e => VPackError.Codec(e.toString).invalid, _.valid
    )
  }

}
