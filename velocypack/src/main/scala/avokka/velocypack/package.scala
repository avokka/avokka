package avokka

import cats.implicits._
import scodec.bits.{BitVector, ByteVector}
import scodec.{DecodeResult, Decoder, Encoder}

package object velocypack {

  implicit class SyntaxToVPack[T](v: T) {
    def toVPack(implicit encoder: VPackEncoder[T]): VPackDecoder.Result[ByteVector] = {
      codecs.vpackEncoder.encode(encoder.encode(v)).fold(
        e => VPackError.Codec(e).asLeft, _.bytes.asRight
      )
    }
  }

  implicit class SyntaxFromVPack(bits: BitVector) {
    def fromVPack[T](implicit decoder: VPackDecoder[T]): VPackDecoder.Result[DecodeResult[T]] = {
      codecs.vpackDecoder.decode(bits).fold(
        e => VPackError.Codec(e).asLeft, _.asRight
      ).flatMap { r =>
        decoder.decode(r.value).map(d => DecodeResult(d, r.remainder))
      }
    }
  }

}
