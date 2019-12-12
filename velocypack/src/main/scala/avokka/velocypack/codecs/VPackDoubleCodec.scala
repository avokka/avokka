package avokka.velocypack.codecs

import avokka.velocypack.{VPackDouble, VPackValue}
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{doubleL, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}

/**
 * Codec of double
 *
 * 0x1b : double IEEE-754, 8 bytes follow, stored as little endian uint64 equivalent
 */
object VPackDoubleCodec {
  import VPackType.DoubleType

  val encoder: Encoder[VPackDouble] = new Encoder[VPackDouble] {
    override def sizeBound: SizeBound = SizeBound.exact(8 + 64)
    override def encode(v: VPackDouble): Attempt[BitVector] = for {
      bits <- doubleL.encode(v.value)
    } yield DoubleType.bits ++ bits
  }

  val decoder: Decoder[VPackDouble] = doubleL.map(VPackDouble.apply)

  val codec: Codec[VPackDouble] = Codec(encoder, vpackDecoder.emap({
    case v: VPackDouble => v.pure[Attempt]
    case _ => Err("not a vpack double").raiseError
  }))
}
