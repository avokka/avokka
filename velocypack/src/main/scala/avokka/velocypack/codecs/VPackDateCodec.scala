package avokka.velocypack
package codecs

import cats.syntax.applicative._
import cats.syntax.applicativeError._
import scodec.bits.BitVector
import scodec.codecs.int64L
import scodec.interop.cats._
import scodec.{Attempt, Codec, Decoder, Encoder, Err, SizeBound}
import VPackType.DateType
import VPack.VDate

/**
  * Codec of date
  *
  * 0x1c : UTC-date in milliseconds since the epoch, stored as 8 byte signed int, little endian, two's complement
  */
private[codecs] object VPackDateCodec {

  private[codecs] val encoder: Encoder[VDate] = new Encoder[VDate] {
    override def sizeBound: SizeBound = SizeBound.exact(8 + 64)

    override def encode(v: VDate): Attempt[BitVector] =
      for {
        bits <- int64L.encode(v.value)
      } yield DateType.bits ++ bits
  }

  private[codecs] val decoder: Decoder[VDate] = int64L.map(VDate.apply)

  private[codecs] val codec: Codec[VDate] = Codec(encoder, vpackDecoder.emap({
    case v: VDate => v.pure[Attempt]
    case _        => Err("not a vpack date").raiseError[Attempt, VDate]
  }))
}
