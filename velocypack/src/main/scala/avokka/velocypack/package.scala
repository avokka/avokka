package avokka

import cats.ApplicativeThrow
import cats.data.StateT
import cats.syntax.either._
import scodec.{Attempt, DecodeResult, Decoder}
import scodec.bits.BitVector

package object velocypack extends ShowInstances {

  /** return type of decoding a VPack to a T */
  private[velocypack] type Result[T] = Either[VPackError, T]

  implicit final class SyntaxToVPack[T](private val value: T) extends AnyVal {

    /**
      * transforms value to vpack value
      * @param encoder implicit encoder
      * @return vpack value
      */
    def toVPack(implicit encoder: VPackEncoder[T]): VPack = encoder.encode(value)

    /**
      * transforms value to vpack bitvector
      * @param encoder implicit encoder
      * @return
      */
    def toVPackBits(implicit encoder: VPackEncoder[T]): Result[BitVector] = encoder.bits(value)
  }

  implicit final class SyntaxFromVPackBits(private val bits: BitVector) extends AnyVal {

    /** decodes to vpack value (internal use only because this looses remainder) */
    private[avokka] def asVPackValue: Result[VPack] = codecs.vpackDecoder
      .decodeValue(bits)
      .toEither
      .leftMap(VPackError.Codec)

    /**
      * decodes vpack bitvector to T
      * @param decoder implicit decoder
      * @tparam T decoded type
      * @return either error or (T value and remainder)
      */
    def asVPack[T](implicit decoder: VPackDecoder[T]): Result[DecodeResult[T]] = decoder.decode(bits)
  }

  implicit final class DecoderStateOps[T](private val decoder: Decoder[T]) extends AnyVal {
    def asState[F[_]](implicit F: ApplicativeThrow[F]): StateT[F, BitVector, T] = StateT { bits: BitVector =>
      decoder.decode(bits) match {
        case Attempt.Successful(result) => F.pure(result.remainder -> result.value)
        case Attempt.Failure(cause) => F.raiseError(VPackError.Codec(cause))
      }
    }
  }
}
