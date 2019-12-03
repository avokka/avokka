package avokka.velocypack.codecs

import scodec.bits.BitVector
import scodec.codecs.{fail, longL, provide, uint8L, ulongL, vlongL}
import scodec.{Attempt, DecodeResult, Decoder, Err}

/**
 * Decodes the head byte and the total length in bytes of a velocypack value
 */
private object VPackHeadLengthDecoder extends Decoder[HeadLength] {

  /**
   * determines the decoder for byte length from the head byte
   */
  private val vpLengthCodecs: Map[Int, Decoder[Long]] = {
    Map(
//      0x00 : none
      VPackArrayCodec.emptyByte -> provide(1L),
      0x02 -> ulongL(8),
      0x03 -> ulongL(16),
      0x04 -> ulongL(32),
      0x05 -> longL(64),
      0x06 -> ulongL(8),
      0x07 -> ulongL(16),
      0x08 -> ulongL(32),
      0x09 -> longL(64),
      VPackObjectCodec.emptyByte -> provide(1L),
      0x0b -> ulongL(8),
      0x0c -> ulongL(16),
      0x0d -> ulongL(32),
      0x0e -> longL(64),
      0x0f -> ulongL(8),
      0x10 -> ulongL(16),
      0x11 -> ulongL(32),
      0x12 -> longL(64),
      VPackArrayCodec.compactByte  -> vlongL,
      VPackObjectCodec.compactByte -> vlongL,
      // 0x15 - 0x16 : reserved
      VPackIllegalCodec.headByte   -> provide(1L),
      VPackNullCodec.headByte      -> provide(1L),
      VPackBooleanCodec.falseByte  -> provide(1L),
      VPackBooleanCodec.trueByte   -> provide(1L),
      VPackDoubleCodec.headByte    -> provide(1L + 8),
      VPackDateCodec.headByte      -> provide(1L + 8),
      // 0x1d : external
      VPackMinKeyCodec.headByte    -> provide(1L),
      VPackMaxKeyCodec.headByte    -> provide(1L),
    ) ++
      (for { x <- 0x20 to 0x27 } yield x -> provide(1L + x.toLong - 0x1f)) ++
      (for { x <- 0x28 to 0x2f } yield x -> provide(1L + x.toLong - 0x27)) ++
      (for { x <- 0x30 to 0x3f } yield x -> provide(1L)) ++
      (for {
        h <- VPackStringCodec.smallByte until VPackStringCodec.longByte
      } yield h -> provide(1L + h.toLong - VPackStringCodec.smallByte)) ++
      Map(
        VPackStringCodec.longByte -> longL(64).map(1L + 8 + _),
      ) ++
      (for {
        h <- VPackBinaryCodec.minByte to VPackBinaryCodec.maxByte
        l = h - VPackBinaryCodec.minByte + 1
      } yield h -> ulongLA(8 * l).map(1L + l + _))
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[HeadLength]] = {
    for {
      head <- uint8L.decode(bits)
      len  <- vpLengthCodecs.getOrElse(head.value, fail(Err(s"unknown vpack header '${head.value.toHexString}'"))).decode(head.remainder)
    } yield len.map(l => HeadLength(head.value, l))
  }
}
