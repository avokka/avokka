package avokka.velocypack.codecs

import avokka.velocypack.{VPackDate, VPackValue}
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{int64L, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}

/**
 * Codec of date
 *
 * 0x1c : UTC-date in milliseconds since the epoch, stored as 8 byte signed int, little endian, two's complement
 */
object VPackDateCodec {
  val headByte = 0x1c

  val encoder: Encoder[VPackDate] = new Encoder[VPackDate] {
    override def sizeBound: SizeBound = SizeBound.exact(8 + 64)

    override def encode(v: VPackDate): Attempt[BitVector] = for {
      bits <- int64L.encode(v.value)
    } yield BitVector(headByte) ++ bits
  }

  val decoder: Decoder[VPackDate] = int64L.map(VPackDate.apply)

  val codec: Codec[VPackDate] = Codec(encoder, VPackValue.vpackDecoder.emap({
    case v: VPackDate => v.pure[Attempt]
    case _ => Err("not a vpack date").raiseError
  }))
}
