package avokka.velocypack
package codecs

import cats.syntax.applicative._
import cats.syntax.applicativeError._
import scodec.bits.BitVector
import scodec.codecs.{longL, ulongL}
import scodec.interop.cats._
import scodec.{Attempt, Codec, Decoder, Encoder, Err, SizeBound}
import VPackType.{IntSignedType, IntUnsignedType}
import VPack.VLong

/**
  * Codec of ints
  *
  * 0x20-0x27 : signed int, little endian, 1 to 8 bytes, number is V - 0x1f, two's complement
  *
  * 0x28-0x2f : uint, little endian, 1 to 8 bytes, number is V - 0x27
  */
private object VPackLongCodec {

  private[codecs] val encoder: Encoder[VLong] = new Encoder[VLong] {
    override def sizeBound: SizeBound = SizeBound.bounded(8 + 8, 8 + 64)

    override def encode(v: VLong): Attempt[BitVector] = {
      v.value match {
        // negative as signed
        case s if s < 0 && s >= -(1L << 7)  => longL(8).encode(s).map  { BitVector(0x20) ++ _ }
        case s if s < 0 && s >= -(1L << 15) => longL(16).encode(s).map { BitVector(0x21) ++ _ }
        case s if s < 0 && s >= -(1L << 23) => longL(24).encode(s).map { BitVector(0x22) ++ _ }
        case s if s < 0 && s >= -(1L << 31) => longL(32).encode(s).map { BitVector(0x23) ++ _ }
        case s if s < 0 && s >= -(1L << 39) => longL(40).encode(s).map { BitVector(0x24) ++ _ }
        case s if s < 0 && s >= -(1L << 47) => longL(48).encode(s).map { BitVector(0x25) ++ _ }
        case s if s < 0 && s >= -(1L << 55) => longL(56).encode(s).map { BitVector(0x26) ++ _ }
        case s if s < 0                     => longL(64).encode(s).map { BitVector(0x27) ++ _ }
        // positive as unsigned
        case u if u >= 0 && u < (1L << 8) => ulongL(8).encode(u).map  { BitVector(0x28) ++ _ }
        case u if u > 0 && u < (1L << 16) => ulongL(16).encode(u).map { BitVector(0x29) ++ _ }
        case u if u > 0 && u < (1L << 24) => ulongL(24).encode(u).map { BitVector(0x2a) ++ _ }
        case u if u > 0 && u < (1L << 32) => ulongL(32).encode(u).map { BitVector(0x2b) ++ _ }
        case u if u > 0 && u < (1L << 40) => ulongL(40).encode(u).map { BitVector(0x2c) ++ _ }
        case u if u > 0 && u < (1L << 48) => ulongL(48).encode(u).map { BitVector(0x2d) ++ _ }
        case u if u > 0 && u < (1L << 56) => ulongL(56).encode(u).map { BitVector(0x2e) ++ _ }
        case u if u > 0                   => longL(64).encode(u).map  { BitVector(0x2f) ++ _ }
      }
    }
  }

  /** patch scodec-bits toLong not sign shifting for large negative longs */
  private def longLpatch(bits: Int)(l: Long): Long = {
    if (((1L << (bits - 1)) & l) != 0) {
      val shift = 64 - bits
      (l << shift) >> shift
    } else l
  }

  private[codecs] def decoderSigned(t: IntSignedType): Decoder[VLong] =
    for {
      l <- longL(8 * t.lengthSize).map(longLpatch(8 * t.lengthSize))
    } yield VLong(l)

  private[codecs] def decoderUnsigned(t: IntUnsignedType): Decoder[VLong] =
    for {
      l <- ulongLA(8 * t.lengthSize)
    } yield VLong(l)

  private[codecs] val codec: Codec[VLong] = Codec(encoder, vpackDecoder.emap({
    case v: VLong => v.pure[Attempt]
    case _        => Err("not a vpack long").raiseError
  }))

}
