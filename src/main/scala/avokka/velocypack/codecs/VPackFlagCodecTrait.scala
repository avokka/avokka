package avokka.velocypack.codecs

import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.uint8L
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

/**
 * Codec needing only his head to provide a value
 *
 * @tparam T
 */
trait VPackFlagCodecTrait[T] extends Codec[T] {
  override def sizeBound: SizeBound = SizeBound.exact(8)

  def headByte: Int
  def provide: Int => T
  def errorMessage: String

  override def encode(value: T): Attempt[BitVector] = BitVector(headByte).pure[Attempt]

  override def decode(bits: BitVector): Attempt[DecodeResult[T]] = for {
    head <- uint8L.decode(bits).ensure(Err(errorMessage))(_.value == headByte)
  } yield head.map(provide)
}
