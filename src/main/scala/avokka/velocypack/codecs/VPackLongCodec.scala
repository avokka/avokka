package avokka.velocypack.codecs

import avokka.velocypack.VPackLong
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{longL, provide, uint8L, ulongL}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}

object VPackLongCodec extends Codec[VPackLong] with VPackNumericCodec[Long] {

  override def sizeBound: SizeBound = SizeBound.bounded(8, 8 + 64)

  private val smallEncoder = smallEncode.andThen(b => BitVector(b).pure[Attempt])

  private val encoders: PartialFunction[Long, Attempt[BitVector]] = {
    // negative as signed
    case s if s < 0 && s >= -(1L << 7)  => longL( 8).encode(s).map { BitVector(0x20) ++ _ }
    case s if s < 0 && s >= -(1L << 15) => longL(16).encode(s).map { BitVector(0x21) ++ _ }
    case s if s < 0 && s >= -(1L << 23) => longL(24).encode(s).map { BitVector(0x22) ++ _ }
    case s if s < 0 && s >= -(1L << 31) => longL(32).encode(s).map { BitVector(0x23) ++ _ }
    case s if s < 0 && s >= -(1L << 39) => longL(40).encode(s).map { BitVector(0x24) ++ _ }
    case s if s < 0 && s >= -(1L << 47) => longL(48).encode(s).map { BitVector(0x25) ++ _ }
    case s if s < 0 && s >= -(1L << 55) => longL(56).encode(s).map { BitVector(0x26) ++ _ }
    case s if s < 0 => longL(64).encode(s).map { BitVector(0x27) ++ _ }
    // positive as unsigned
    case u if u > 0 && u < (1L << 8)  => ulongL( 8).encode(u).map { BitVector(0x28) ++ _ }
    case u if u > 0 && u < (1L << 16) => ulongL(16).encode(u).map { BitVector(0x29) ++ _ }
    case u if u > 0 && u < (1L << 24) => ulongL(24).encode(u).map { BitVector(0x2a) ++ _ }
    case u if u > 0 && u < (1L << 32) => ulongL(32).encode(u).map { BitVector(0x2b) ++ _ }
    case u if u > 0 && u < (1L << 40) => ulongL(40).encode(u).map { BitVector(0x2c) ++ _ }
    case u if u > 0 && u < (1L << 48) => ulongL(48).encode(u).map { BitVector(0x2d) ++ _ }
    case u if u > 0 && u < (1L << 56) => ulongL(56).encode(u).map { BitVector(0x2e) ++ _ }
    case u if u > 0 => longL(64).encode(u).map { BitVector(0x2f) ++ _ }
  }

  override def encode(v: VPackLong): Attempt[BitVector] = {
    smallEncoder.orElse(encoders)(v.value)
  }

  private val decoder = smallDecode.andThen(provide).orElse[Int, Decoder[Long]]({
    // signed
    case 0x20 => longL(8)
    case 0x21 => longL(16)
    case 0x22 => longL(24)
    case 0x23 => longL(32)
    case 0x24 => longL(40)
    case 0x25 => longL(48)
    case 0x26 => longL(56)
    case 0x27 => longL(64)
    // unsigned
    case 0x28 => ulongL(8)
    case 0x29 => ulongL(16)
    case 0x2a => ulongL(24)
    case 0x2b => ulongL(32)
    case 0x2c => ulongL(40)
    case 0x2d => ulongL(48)
    case 0x2e => ulongL(56)
    case 0x2f => longL(64)
  })

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackLong]] = {
    for {
      head  <- uint8L.decode(bits).ensure(Err("not a vpack long"))(h => decoder.isDefinedAt(h.value))
      value <- decoder(head.value).decode(head.remainder)
    } yield value.map(VPackLong.apply)
  }
}
