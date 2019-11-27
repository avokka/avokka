package avokka.velocypack.codecs

import avokka.velocypack.VPackDate
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{int64L, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

object VPackDateCodec extends Codec[VPackDate] {
  override def sizeBound: SizeBound = SizeBound.exact(8 + 64)

  val headByte = 0x1c

  override def encode(value: VPackDate): Attempt[BitVector] = for {
    bits <- int64L.encode(value.value)
  } yield BitVector(headByte) ++ bits

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackDate]] = for {
    head <- uint8L.decode(bits).ensure(Err("not a vpack date"))(_.value == headByte)
    v    <- int64L.decode(head.remainder)
  } yield v.map(VPackDate.apply)

}
