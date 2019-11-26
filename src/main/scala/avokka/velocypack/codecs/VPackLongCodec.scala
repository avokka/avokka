package avokka.velocypack.codecs

import avokka.velocypack.VPackLong
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{fail, longL, provide, uint8L, ulongL}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

object VPackLongCodec extends Codec[VPackLong] {

  override def sizeBound: SizeBound = SizeBound.exact(8 * (1 + 8))

  override def encode(value: VPackLong): Attempt[BitVector] = {
    value.value match {
      // small ints
      case 0 => Attempt.successful(BitVector(0x30))
      case 1 => Attempt.successful(BitVector(0x31))
      case 2 => Attempt.successful(BitVector(0x32))
      case 3 => Attempt.successful(BitVector(0x33))
      case 4 => Attempt.successful(BitVector(0x34))
      case 5 => Attempt.successful(BitVector(0x35))
      case 6 => Attempt.successful(BitVector(0x36))
      case 7 => Attempt.successful(BitVector(0x37))
      case 8 => Attempt.successful(BitVector(0x38))
      case 9 => Attempt.successful(BitVector(0x39))
      case -6 => Attempt.successful(BitVector(0x3a))
      case -5 => Attempt.successful(BitVector(0x3b))
      case -4 => Attempt.successful(BitVector(0x3c))
      case -3 => Attempt.successful(BitVector(0x3d))
      case -2 => Attempt.successful(BitVector(0x3e))
      case -1 => Attempt.successful(BitVector(0x3f))
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
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackLong]] = {
    for {
      head  <- uint8L.decode(bits).ensure(Err("not a vpack number"))(h => h.value >= 0x20 && h.value <= 0x3f)
      value <- (head.value match {
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
        // small ints
        case 0x30 => provide(0L)
        case 0x31 => provide(1L)
        case 0x32 => provide(2L)
        case 0x33 => provide(3L)
        case 0x34 => provide(4L)
        case 0x35 => provide(5L)
        case 0x36 => provide(6L)
        case 0x37 => provide(7L)
        case 0x38 => provide(8L)
        case 0x39 => provide(9L)
        case 0x3a => provide(-6L)
        case 0x3b => provide(-5L)
        case 0x3c => provide(-4L)
        case 0x3d => provide(-3L)
        case 0x3e => provide(-2L)
        case 0x3f => provide(-1L)
      }).decode(head.remainder)
    } yield value.map(VPackLong.apply)
  }
}
