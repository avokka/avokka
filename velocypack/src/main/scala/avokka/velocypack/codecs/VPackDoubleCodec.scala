package avokka.velocypack.codecs

import avokka.velocypack.VPack.VDouble
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import scodec.bits.BitVector
import scodec.codecs.doubleL
import scodec.interop.cats._
import scodec.{Attempt, Codec, Decoder, Encoder, Err, SizeBound}

/**
 * Codec of double
 *
 * 0x1b : double IEEE-754, 8 bytes follow, stored as little endian uint64 equivalent
 */
object VPackDoubleCodec {
  import VPackType.DoubleType

  val encoder: Encoder[VDouble] = new Encoder[VDouble] {
    override def sizeBound: SizeBound = SizeBound.exact(8 + 64)
    override def encode(v: VDouble): Attempt[BitVector] = for {
      bits <- doubleL.encode(v.value)
    } yield DoubleType.bits ++ bits
  }

  val decoder: Decoder[VDouble] = doubleL.map(VDouble.apply)

  val codec: Codec[VDouble] = Codec(encoder, vpackDecoder.emap({
    case v: VDouble => v.pure[Attempt]
    case _ => Err("not a vpack double").raiseError
  }))
}
