package avokka

import scodec.bits.BitVector
import scodec.{Attempt, DecodeResult, Decoder, Encoder, Err}

package object velocypack {

  implicit def vpEncoder[T](implicit encoder: VPackEncoder[T]): Encoder[T] = codecs.vpackEncoder.contramap(encoder.encode)

  implicit def vpDecoder[T](implicit decoder: VPackDecoder[T]): Decoder[T] = new Decoder[T] {
    override def decode(bits: BitVector): Attempt[DecodeResult[T]] = codecs.vpackDecoder.decode(bits).flatMap { r =>
      decoder.decode(r.value).fold(
        e => Attempt.failure(Err(e.getMessage)),
        t => Attempt.successful(DecodeResult(t, r.remainder))
      )
    }
  }

  implicit class SyntaxToVPack[T](v: T) {
    def toVPack(implicit encoder: VPackEncoder[T]): VPack = {
      encoder.encode(v)
    }
  }
/*
  codecs.vpackEncoder.encode(encoder.encode(v)).fold(
    e => VPackError.Codec(e).asLeft, _.bytes.asRight
  )
      codecs.vpackDecoder.decode(bits).fold(
        e => VPackError.Codec(e).asLeft, _.asRight
      ).flatMap { r =>
        decoder.decode(r.value).map(d => DecodeResult(d, r.remainder))
      }
*/
  implicit class SyntaxFromVPack(v: VPack) {
    def as[T](implicit decoder: VPackDecoder[T]): VPackDecoder.Result[T] = {
      decoder.decode(v)
    }
  }

}
