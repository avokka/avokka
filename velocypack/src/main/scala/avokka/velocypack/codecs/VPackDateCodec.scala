package avokka.velocypack.codecs

import avokka.velocypack.VPackDate
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{int64L, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

/**
 * Codec of date
 *
 * 0x1c : UTC-date in milliseconds since the epoch, stored as 8 byte signed int, little endian, two's complement
 */
object VPackDateCodec extends Codec[VPackDate] {
  override def sizeBound: SizeBound = SizeBound.exact(8 + 64)

  val headByte = 0x1c

  override def encode(v: VPackDate): Attempt[BitVector] = for {
    bits <- int64L.encode(v.value)
  } yield BitVector(headByte) ++ bits

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackDate]] = for {
    head <- uint8L.decode(bits).ensure(Err("not a vpack date"))(_.value == headByte)
    v    <- int64L.decode(head.remainder)
  } yield v.map(VPackDate.apply)

}