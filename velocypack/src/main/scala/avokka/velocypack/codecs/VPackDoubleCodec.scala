package avokka.velocypack.codecs

import avokka.velocypack.VPackDouble
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{doubleL, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

/**
 * Codec of double
 *
 * 0x1b : double IEEE-754, 8 bytes follow, stored as little endian uint64 equivalent
 */
object VPackDoubleCodec extends Codec[VPackDouble] {
  override def sizeBound: SizeBound = SizeBound.exact(8 + 64)

  val headByte = 0x1b

  override def encode(v: VPackDouble): Attempt[BitVector] = for {
    bits <- doubleL.encode(v.value)
  } yield BitVector(headByte) ++ bits

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackDouble]] = for {
    head <- uint8L.decode(bits).ensure(Err("not a vpack double"))(h => h.value == headByte)
    v    <- doubleL.decode(head.remainder)
  } yield v.map(VPackDouble.apply)

}
