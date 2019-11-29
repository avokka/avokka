package avokka.velocypack.codecs

import avokka.velocypack.VPackNull
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}
import scodec.bits.BitVector
import scodec.codecs.uint8L
import cats.implicits._
import scodec.interop.cats._

object VPackNullCodec extends Codec[VPackNull.type] {
  override def sizeBound: SizeBound = SizeBound.exact(8)

  val headByte = 0x18

  override def encode(value: VPackNull.type): Attempt[BitVector] = BitVector(headByte).pure[Attempt]

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackNull.type]] = for {
    head <- uint8L.decode(bits).ensure(Err("not vpack null"))(_.value == headByte)
  } yield head.map(_ => VPackNull)
}
