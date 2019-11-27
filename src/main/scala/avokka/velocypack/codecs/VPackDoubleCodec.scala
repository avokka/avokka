package avokka.velocypack.codecs

import avokka.velocypack.VPackDouble
import scodec.{Attempt, Codec, DecodeResult, Decoder, Err, SizeBound}
import scodec.bits.BitVector
import scodec.codecs.{doubleL, provide, uint8L}
import cats.implicits._
import scodec.interop.cats._

object VPackDoubleCodec extends Codec[VPackDouble] with VPackNumericCodec[Double] {
  override def sizeBound: SizeBound = SizeBound.bounded(8, 8 + 64)

  val headByte = 0x1b

  private val smallEncoder = smallEncode.andThen(b => BitVector(b).pure[Attempt])

  override def encode(v: VPackDouble): Attempt[BitVector] = smallEncoder.applyOrElse(v.value, (d: Double) =>
    for {
      bits <- doubleL.encode(d)
    } yield BitVector(headByte) ++ bits
  )

  private val decoder = smallDecode.andThen(provide).orElse[Int, Decoder[Double]]({
    case `headByte` => doubleL
  })

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackDouble]] = for {
    head <- uint8L.decode(bits).ensure(Err("not a vpack double"))(h => decoder.isDefinedAt(h.value))
    v    <- decoder(head.value).decode(head.remainder)
  } yield v.map(VPackDouble.apply)

}
