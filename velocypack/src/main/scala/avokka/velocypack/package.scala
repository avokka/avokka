package avokka

import cats.data.StateT
import cats.syntax.bifunctor._
import cats.syntax.traverse._
import scodec.interop.cats._
import scodec.bits.BitVector
import scodec.{Attempt, DecodeResult, Decoder}

package object velocypack {

  /** return type of decoding a VPack to a T */
  type VPackResult[T] = Either[VPackError, T]

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
    def toVPackBits(implicit encoder: VPackEncoder[T]): VPackResult[BitVector] = encoder.bits(value)
  }

  implicit final class SyntaxFromVPackBits(private val bits: BitVector) extends AnyVal {

    /** decodes to vpack value (internal use only because this looses remainder) */
    private[avokka] def asVPackValue: VPackResult[VPack] = codecs.vpackDecoder
      .decodeValue(bits)
      .toEither
      .leftMap(VPackError.Codec.apply)

    /**
      * decodes vpack bitvector to T
      * @param decoder implicit decoder
      * @tparam T decoded type
      * @return either error or (T value and remainder)
      */
    def asVPack[T](implicit decoder: VPackDecoder[T]): VPackResult[DecodeResult[T]] = {
      codecs.vpackDecoder
        .decode(bits)
        .toEither
        .leftMap(VPackError.Codec.apply)
        .flatMap(_.traverse(decoder.decode))
    }

    /**
      * repeatedly decodes vpack bitvector to vector of T
      * @param decoder implicit element decoder
      * @tparam T decoded type
      * @return either err or (Vector[T] and remainder)
      */
    def asVPackSequence[T](implicit decoder: VPackDecoder[T]): VPackResult[DecodeResult[Vector[T]]] = {
      codecs.vpackDecoder
        .collect[Vector, VPack](bits, None)
        .toEither
        .leftMap(VPackError.Codec.apply(_))
        .flatMap(_.traverse(_.traverse(decoder.decode)))
    }
  }

  implicit final class DecoderOps[T](val decoder: Decoder[T]) extends DecoderOpsShims[T] {

    def asState: StateT[VPackResult, BitVector, T] = StateT { (bits: BitVector) =>
      decoder.decode(bits) match {
        case Attempt.Successful(result) => Right(result.remainder -> result.value)
        case Attempt.Failure(cause) => Left(VPackError.Codec(cause))
      }
    }

  }

}
