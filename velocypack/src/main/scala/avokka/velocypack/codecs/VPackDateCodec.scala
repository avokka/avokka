package avokka.velocypack.codecs

import avokka.velocypack.{VPackDate, VPackValue}
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.int64L
import scodec.interop.cats._
import scodec.{Attempt, Codec, Decoder, Encoder, Err, SizeBound}

/**
 * Codec of date
 *
 * 0x1c : UTC-date in milliseconds since the epoch, stored as 8 byte signed int, little endian, two's complement
 */
object VPackDateCodec {
  import VPackType.DateType

  val encoder: Encoder[VPackDate] = new Encoder[VPackDate] {
    override def sizeBound: SizeBound = SizeBound.exact(8 + 64)

    override def encode(v: VPackDate): Attempt[BitVector] = for {
      bits <- int64L.encode(v.value)
    } yield DateType.bits ++ bits
  }

  val decoder: Decoder[VPackDate] = int64L.map(VPackDate.apply)

  val codec: Codec[VPackDate] = Codec(encoder, vpackDecoder.emap({
    case v: VPackDate => v.pure[Attempt]
    case _ => Err("not a vpack date").raiseError
  }))
}
