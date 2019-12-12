package avokka.velocypack.codecs

import avokka.velocypack.VPackSmallint
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{provide, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}

/**
 * Codec of small ints
 *
 * 0x30-0x39 : small integers 0, 1, ... 9
 *
 * 0x3a-0x3f : small negative integers -6, -5, ..., -1
 */
object VPackSmallintCodec {
  import VPackType.{SmallintPositiveType, SmallintNegativeType}

  val encoder: Encoder[VPackSmallint] = new Encoder[VPackSmallint] {
    override def sizeBound: SizeBound = SizeBound.exact(8)

    override def encode(v: VPackSmallint): Attempt[BitVector] = {
      val t = if (v.value < 0) SmallintNegativeType(SmallintNegativeType.topByte + v.value)
              else SmallintPositiveType(SmallintPositiveType.minByte + v.value)
      t.bits.pure[Attempt]
    }
  }

  def decoderPositive(t: SmallintPositiveType): Decoder[VPackSmallint] = provide(
    VPackSmallint((t.head - SmallintPositiveType.minByte).toByte)
  )
  def decoderNegative(t: SmallintNegativeType): Decoder[VPackSmallint] = provide(
    VPackSmallint((t.head - SmallintNegativeType.topByte).toByte)
  )


}

