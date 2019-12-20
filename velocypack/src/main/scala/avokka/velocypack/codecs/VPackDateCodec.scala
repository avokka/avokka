package avokka.velocypack.codecs

import avokka.velocypack.VPack.VDate
import cats.syntax.applicative._
import cats.syntax.applicativeError._
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

  val encoder: Encoder[VDate] = new Encoder[VDate] {
    override def sizeBound: SizeBound = SizeBound.exact(8 + 64)

    override def encode(v: VDate): Attempt[BitVector] =
      for {
        bits <- int64L.encode(v.value)
      } yield DateType.bits ++ bits
  }

  val decoder: Decoder[VDate] = int64L.map(VDate.apply)

  val codec: Codec[VDate] = Codec(encoder, vpackDecoder.emap({
    case v: VDate => v.pure[Attempt]
    case _        => Err("not a vpack date").raiseError
  }))
}
