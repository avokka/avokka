package avokka.velocypack.codecs

import avokka.velocypack.VPackString
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{fixedSizeBytes, int64L, uint8L, utf8}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

object VPackStringCodec extends Codec[VPackString] {
  override def sizeBound: SizeBound = SizeBound.atLeast(8)

  override def encode(value: VPackString): Attempt[BitVector] = {
    for {
      bs    <- utf8.encode(value.value)
      len   = bs.size / 8
      head  = if (len > 126) BitVector(0xbf) ++ ulongBytes(len, 8)
              else BitVector(0x40 + len)
    } yield head ++ bs
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackString]] = {
    for {
      head <- uint8L.decode(bits).ensure(Err("not a vpack string"))(h => h.value >= 0x40 && h.value <= 0xbf)
      len  <- if (head.value == 0xbf) int64L.decode(head.remainder)
              else head.map(_.toLong - 0x40).pure[Attempt]
      str  <- fixedSizeBytes(len.value, utf8).decode(len.remainder)
    } yield str.map(VPackString.apply)
  }
}
