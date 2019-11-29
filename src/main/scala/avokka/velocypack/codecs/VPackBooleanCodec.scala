package avokka.velocypack.codecs

import avokka.velocypack.VPackBoolean
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.uint8L
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

/**
 * Codec of bool
 *
 * 0x19 : false
 * 0x1a : true
 */
object VPackBooleanCodec extends Codec[VPackBoolean] {
  override def sizeBound: SizeBound = SizeBound.exact(8)

  val trueByte = 0x1a
  val falseByte = 0x19

  override def encode(v: VPackBoolean): Attempt[BitVector] = {
    BitVector(if (v.value) trueByte else falseByte).pure[Attempt]
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackBoolean]] = {
    for {
      head <- uint8L.decode(bits).ensure(Err("not a vpack boolean"))(h => h.value == falseByte || h.value == trueByte)
    } yield head.map(h => VPackBoolean(h == trueByte))
  }
}
