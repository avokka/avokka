package avokka

import cats.data.Validated
import cats.implicits._
import scodec.bits.BitVector
import scodec.{DecodeResult, Decoder, Encoder}

package object velocypack extends CodecImplicits {

  implicit class SyntaxToVPack[T](v: T) {
    def toVPack(implicit encoder: Encoder[T]): Validated[VPackError, BitVector] = encoder.encode(v).fold(
      err => VPackErrorCodec(err.toString).invalid, _.valid
    )
  }

  implicit class SyntaxFromVPack(bits: BitVector) {
    def fromVPack[T](implicit decoder: Decoder[T]): Validated[VPackError, DecodeResult[T]] = decoder.decode(bits).fold(
      err => VPackErrorCodec(err.toString).invalid, _.valid
    )
  }

}
