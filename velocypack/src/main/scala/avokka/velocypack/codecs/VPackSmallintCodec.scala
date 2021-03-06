package avokka.velocypack
package codecs

import cats.syntax.applicative._
import scodec.bits.BitVector
import scodec.codecs.provide
import scodec.interop.cats._
import scodec.{Attempt, Decoder, Encoder, SizeBound}
import VPackType.{SmallintNegativeType, SmallintPositiveType}

/**
  * Codec of small ints
  *
  * 0x30-0x39 : small integers 0, 1, ... 9
  *
  * 0x3a-0x3f : small negative integers -6, -5, ..., -1
  */
private[codecs] object VPackSmallintCodec {

  private[codecs] val encoder: Encoder[VSmallint] = new Encoder[VSmallint] {
    override def sizeBound: SizeBound = SizeBound.exact(8)

    override def encode(v: VSmallint): Attempt[BitVector] = {
      val t =
        if (v.value < 0) SmallintNegativeType(SmallintNegativeType.topByte + v.value)
        else SmallintPositiveType(SmallintPositiveType.minByte + v.value)
      t.bits.pure[Attempt]
    }
  }

  private[codecs] def decoderPositive(t: SmallintPositiveType): Decoder[VSmallint] = provide(
    VSmallint((t.header - SmallintPositiveType.minByte).toByte)
  )
  private[codecs] def decoderNegative(t: SmallintNegativeType): Decoder[VSmallint] = provide(
    VSmallint((t.header - SmallintNegativeType.topByte).toByte)
  )

}
