package avokka.velocypack.codecs

import avokka.velocypack.VPackDouble
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}
import scodec.bits.BitVector
import scodec.codecs.{doubleL, uint8L}
import cats.implicits._
import scodec.interop.cats._

object VPackDoubleCodec extends Codec[VPackDouble] {
  override def sizeBound: SizeBound = SizeBound.exact(8 + 64)

  val headByte = 0x1b

  override def encode(value: VPackDouble): Attempt[BitVector] = for {
    bits <- doubleL.encode(value.value)
  } yield BitVector(headByte) ++ bits

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackDouble]] = for {
    head <- uint8L.decode(bits).ensure(Err("not a vpack double"))(_.value == headByte)
    v    <- doubleL.decode(head.remainder)
  } yield v.map(VPackDouble.apply)

}
