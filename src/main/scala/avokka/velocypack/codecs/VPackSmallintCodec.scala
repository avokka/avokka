package avokka.velocypack.codecs

import avokka.velocypack.VPackSmallint
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.uint8L
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

/**
 * Codec of small ints
 *
 * 0x30-0x39 : small integers 0, 1, ... 9
 *
 * 0x3a-0x3f : small negative integers -6, -5, ..., -1
 */
object VPackSmallintCodec extends Codec[VPackSmallint] {
  override def sizeBound: SizeBound = SizeBound.exact(8)

  override def encode(v: VPackSmallint): Attempt[BitVector] = {
    BitVector(v.value + (if (v.value < 0) 0x40 else 0x30)).pure[Attempt]
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackSmallint]] = for {
    head  <- uint8L.decode(bits).ensure(Err("not a vpack smallint"))(h => h.value >= 0x30 && h.value <= 0x3f)
  } yield head.map(h => VPackSmallint((h - (if (h < 0x3a) 0x30 else 0x40)).toByte))

}

