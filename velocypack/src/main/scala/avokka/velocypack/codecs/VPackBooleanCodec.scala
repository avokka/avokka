package avokka.velocypack.codecs

import avokka.velocypack.VPack.VBoolean
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import scodec.bits.BitVector
import scodec.interop.cats._
import scodec.{Attempt, Codec, Encoder, Err, SizeBound}

/**
 * Codec of bool
 *
 * 0x19 : false
 * 0x1a : true
 */
object VPackBooleanCodec {
  import VPackType.{TrueType, FalseType}

  val encoder: Encoder[VBoolean] = new Encoder[VBoolean] {
    override def sizeBound: SizeBound = SizeBound.exact(8)

    override def encode(v: VBoolean): Attempt[BitVector] = {
      (if (v.value) TrueType else FalseType).bits.pure[Attempt]
    }
  }

  val codec: Codec[VBoolean] = Codec(encoder, vpackDecoder.emap({
    case v: VBoolean => v.pure[Attempt]
    case _ => Err("not a vpack boolean").raiseError
  }))
}
