package avokka.velocypack
package codecs

import avokka.velocypack.VPack.VBoolean
import avokka.velocypack.codecs.VPackType.{FalseType, TrueType}
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
private[codecs] object VPackBooleanCodec {

  private[codecs] val encoder: Encoder[VBoolean] = new Encoder[VBoolean] {
    override def sizeBound: SizeBound = SizeBound.exact(8)

    override def encode(v: VBoolean): Attempt[BitVector] =
      (if (v.value) TrueType else FalseType).bits.pure[Attempt]

  }

  private[codecs] val codec: Codec[VBoolean] = Codec(encoder, vpackDecoder.emap({
    case v: VBoolean => v.pure[Attempt]
    case _           => Err("not a vpack boolean").raiseError
  }))
}
