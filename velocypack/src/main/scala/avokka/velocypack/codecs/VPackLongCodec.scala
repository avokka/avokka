package avokka.velocypack
package codecs

import cats.syntax.applicative._
import cats.syntax.applicativeError._
import scodec.bits.BitVector
import scodec.codecs.{longL, ulongL}
import scodec.interop.cats._
import scodec.{Attempt, Codec, Decoder, Encoder, Err, SizeBound}
import VPackType.{IntSignedType, IntUnsignedType}

/**
  * Codec of ints
  *
  * 0x20-0x27 : signed int, little endian, 1 to 8 bytes, number is V - 0x1f, two's complement
  *
  * 0x28-0x2f : uint, little endian, 1 to 8 bytes, number is V - 0x27
  */
private[codecs] object VPackLongCodec {

  private[codecs] val encoder: Encoder[VLong] = new Encoder[VLong] {
    override def sizeBound: SizeBound = SizeBound.bounded(8 + 8, 8 + 64)

    // negative as signed
    def encodeNegative(s: Long): Attempt[BitVector] = {
      if      (s >= -(1L << 7))  longL(8).encode(s).map  { BitVector(0x20) ++ _ }
      else if (s >= -(1L << 15)) longL(16).encode(s).map { BitVector(0x21) ++ _ }
      else if (s >= -(1L << 23)) longL(24).encode(s).map { BitVector(0x22) ++ _ }
      else if (s >= -(1L << 31)) longL(32).encode(s).map { BitVector(0x23) ++ _ }
      else if (s >= -(1L << 39)) longL(40).encode(s).map { BitVector(0x24) ++ _ }
      else if (s >= -(1L << 47)) longL(48).encode(s).map { BitVector(0x25) ++ _ }
      else if (s >= -(1L << 55)) longL(56).encode(s).map { BitVector(0x26) ++ _ }
      else                       longL(64).encode(s).map { BitVector(0x27) ++ _ }
    }

    // positive as unsigned
    def encodePositive(u: Long): Attempt[BitVector] = {
      if      (u < (1L << 8))  ulongL(8).encode(u).map  { BitVector(0x28) ++ _ }
      else if (u < (1L << 16)) ulongL(16).encode(u).map { BitVector(0x29) ++ _ }
      else if (u < (1L << 24)) ulongL(24).encode(u).map { BitVector(0x2a) ++ _ }
      else if (u < (1L << 32)) ulongL(32).encode(u).map { BitVector(0x2b) ++ _ }
      else if (u < (1L << 40)) ulongL(40).encode(u).map { BitVector(0x2c) ++ _ }
      else if (u < (1L << 48)) ulongL(48).encode(u).map { BitVector(0x2d) ++ _ }
      else if (u < (1L << 56)) ulongL(56).encode(u).map { BitVector(0x2e) ++ _ }
      else                     longL(64).encode(u).map  { BitVector(0x2f) ++ _ }
    }

    override def encode(v: VLong): Attempt[BitVector] =
      if (v.value < 0) encodeNegative(v.value) else encodePositive(v.value)

  }

  private[codecs] def decoderSigned(t: IntSignedType): Decoder[VLong] =
    for {
      l <- longL(8 * t.lengthSize)
    } yield VLong(l)

  private[codecs] def decoderUnsigned(t: IntUnsignedType): Decoder[VLong] =
    for {
      l <- ulongLA(8 * t.lengthSize)
    } yield VLong(l)

  private[codecs] val codec: Codec[VLong] = Codec(encoder, vpackDecoder.emap({
    case v: VLong => v.pure[Attempt]
    case _        => Err("not a vpack long").raiseError[Attempt, VLong]
  }))

}
