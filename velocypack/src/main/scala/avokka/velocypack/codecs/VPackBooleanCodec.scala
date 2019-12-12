package avokka.velocypack.codecs

import avokka.velocypack.{VPackBoolean, VPackValue}
import cats.implicits._
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

  val encoder: Encoder[VPackBoolean] = new Encoder[VPackBoolean] {
    override def sizeBound: SizeBound = SizeBound.exact(8)

    override def encode(v: VPackBoolean): Attempt[BitVector] = {
      (if (v.value) TrueType else FalseType).bits.pure[Attempt]
    }
  }

  val codec: Codec[VPackBoolean] = Codec(encoder, vpackDecoder.emap({
    case v: VPackBoolean => v.pure[Attempt]
    case _ => Err("not a vpack boolean").raiseError
  }))
}
