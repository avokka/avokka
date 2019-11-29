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

  /*
  def smallEncode[T](implicit num: Numeric[T]): PartialFunction[T, Int] = {
    case 0 => 0x30
    case 1 => 0x31
    case 2 => 0x32
    case 3 => 0x33
    case 4 => 0x34
    case 5 => 0x35
    case 6 => 0x36
    case 7 => 0x37
    case 8 => 0x38
    case 9 => 0x39
    case -6 => 0x3a
    case -5 => 0x3b
    case -4 => 0x3c
    case -3 => 0x3d
    case -2 => 0x3e
    case -1 => 0x3f
  }

  val smallDecode: PartialFunction[Int, Byte] = {
    case 0x30 => 0
    case 0x31 => 1
    case 0x32 => 2
    case 0x33 => 3
    case 0x34 => 4
    case 0x35 => 5
    case 0x36 => 6
    case 0x37 => 7
    case 0x38 => 8
    case 0x39 => 9
    case 0x3a => -6
    case 0x3b => -5
    case 0x3c => -4
    case 0x3d => -3
    case 0x3e => -2
    case 0x3f => -1
  }
   */

}
