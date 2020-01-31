package avokka.velocypack

import cats.syntax.either._
import scodec.DecodeResult
import scodec.bits.BitVector

trait SyntaxImplicits {

  implicit class SyntaxToVPack[T](value: T) {

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

  implicit class SyntaxFromVPackBits(bits: BitVector) {

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

}
