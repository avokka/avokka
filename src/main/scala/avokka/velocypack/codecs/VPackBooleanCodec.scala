package avokka.velocypack.codecs

import avokka.velocypack.VPackBoolean
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.uint8L
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

object VPackBooleanCodec extends Codec[VPackBoolean] {
  override def sizeBound: SizeBound = SizeBound.exact(8)

  override def encode(value: VPackBoolean): Attempt[BitVector] = {
    BitVector(if (value.value) 0x1a else 0x19).pure[Attempt]
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackBoolean]] = {
    for {
      head <- uint8L.decode(bits).ensure(Err("not a vpack boolean"))(h => h.value == 0x19 || h.value == 0x1a)
    } yield head.map(h => VPackBoolean(h == 0x1a))
  }
}
