package avokka.velocypack.codecs

import avokka.velocypack.VPackString
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{fixedSizeBytes, int64L, uint8L, utf8}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

object VPackStringCodec extends Codec[VPackString] {
  override def sizeBound: SizeBound = SizeBound.atLeast(8)

  val shortByte = 0x40
  val longByte  = 0xbf

  override def encode(v: VPackString): Attempt[BitVector] = {
    for {
      bits  <- utf8.encode(v.value)
      len   = bits.size / 8
      head  = if (len > 126) BitVector(longByte) ++ ulongBytes(len, 8)
              else BitVector(shortByte + len)
    } yield head ++ bits
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackString]] = {
    for {
      head <- uint8L.decode(bits).ensure(Err("not a vpack string"))(h => h.value >= shortByte && h.value <= longByte)
      len  <- if (head.value == longByte) int64L.decode(head.remainder)
              else head.map(_.toLong - shortByte).pure[Attempt]
      str  <- fixedSizeBytes(len.value, utf8).decode(len.remainder)
    } yield str.map(VPackString.apply)
  }
}
