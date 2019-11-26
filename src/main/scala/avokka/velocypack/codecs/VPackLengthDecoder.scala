package avokka.velocypack.codecs

import scodec.bits.BitVector
import scodec.codecs.{fail, longL, provide, uint8L, ulongL, vlongL}
import scodec.{Attempt, DecodeResult, Decoder, Err}

object VPackLengthDecoder extends Decoder[Long] {

  lazy val vpLengthCodecs: Map[Int, Decoder[Long]] = {
    Map(
      0x00 -> provide(1L),
      0x01 -> provide(1L),
      0x02 -> ulongL(8),
      0x03 -> ulongL(16),
      0x04 -> ulongL(32),
      0x05 -> longL(64),
      0x06 -> ulongL(8),
      0x07 -> ulongL(16),
      0x08 -> ulongL(32),
      0x09 -> longL(64),
      0x0a -> provide(1L),
      0x0b -> ulongL(8),
      0x0c -> ulongL(16),
      0x0d -> ulongL(32),
      0x0e -> longL(64),
      0x0f -> ulongL(8),
      0x10 -> ulongL(16),
      0x11 -> ulongL(32),
      0x12 -> longL(64),
      0x13 -> vlongL,
      0x14 -> vlongL,
      0x18 -> provide(1L),
      0x19 -> provide(1L),
      0x1a -> provide(1L),
      0x1b -> provide(8L + 1),
      0x1c -> provide(8L + 1),
    ) ++
      (for { x <- 0x20 to 0x27 } yield x -> provide(x.toLong - 0x1f + 1)) ++
      (for { x <- 0x28 to 0x2f } yield x -> provide(x.toLong - 0x27 + 1)) ++
      (for { x <- 0x30 to 0x3f } yield x -> provide(1L)) ++
      (for { x <- 0x40 to 0xbe } yield x -> provide(x.toLong - 0x40 + 1)) ++
      Map(
        0xbf -> longL(64).map(_ + 8 + 1),
      ) ++
      (for { h <- 0xc0 to 0xc7 } yield h -> ulongLA(8 * (h - 0xbf)).map(_ + 2))
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[Long]] = {
    for {
      head <- uint8L.decode(bits)
      len  <- vpLengthCodecs.getOrElse(head.value, fail(Err("unknown vpack header"))).decode(head.remainder)
    } yield len
  }
}
