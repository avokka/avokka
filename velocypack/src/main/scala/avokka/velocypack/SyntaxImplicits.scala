package avokka.velocypack

import cats.instances.either._
import cats.syntax.either._
import cats.syntax.traverse._
import scodec.DecodeResult
import scodec.bits.BitVector
import scodec.interop.cats._

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
    def toVPackBits(implicit encoder: VPackEncoder[T]): Result[BitVector] = {
      val vpack = encoder.encode(value)
      codecs.vpackEncoder.encode(vpack).toEither.leftMap(VPackError.Codec)
    }
  }

  implicit class SyntaxFromVPackBits(bits: BitVector) {

    def asVPack: Result[VPack] = codecs.vpackDecoder.decodeValue(bits).toEither.leftMap(VPackError.Codec)

    /**
      * decodes vpack bitvector to T
      * @param decoder implicit decoder
      * @tparam T decoded type
      * @return either error or (T value and remainder)
      */
    def as[T](implicit decoder: VPackDecoder[T]): Result[DecodeResult[T]] = {
      codecs.vpackDecoder
        .decode(bits)
        .toEither
        .leftMap(VPackError.Codec)
        .flatMap(_.traverse(decoder.decode))
    }
  }

}
