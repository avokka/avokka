package avokka.velocypack

import java.time.Instant

import avokka.velocypack.codecs.between
import cats.data._
import cats.implicits._
import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.cats._

import scala.Int

sealed trait VPackValue {
}

case object VPackReserved1 extends VPackValue {
  implicit val codec: Codec[VPackReserved1.type] = constant(0x15) ~> provide(VPackReserved1)
}

case object VPackReserved2 extends VPackValue {
  implicit val codec: Codec[VPackReserved2.type] = constant(0x16) ~> provide(VPackReserved2)
}

case object VPackIllegal extends VPackValue {
  implicit val codec: Codec[VPackIllegal.type] = constant(0x17) ~> provide(VPackIllegal)
}

case object VPackNull extends VPackValue {
  implicit val codec: Codec[VPackNull.type] = constant(0x18) ~> provide(VPackNull)
}

case class VPackBoolean(value: Boolean) extends VPackValue

object VPackBoolean {
  val encoder: Encoder[VPackBoolean] = Encoder { b =>
    Attempt.successful(BitVector(if (b.value) 0x1a else 0x19))
  }

  val decoder: Decoder[VPackBoolean] = for {
    head  <- uint8L
    value <- head match {
      case 0x19 => provide(false)
      case 0x1a => provide(true)
      case _ => fail(Err("not a vpack boolean"))
    }
  } yield VPackBoolean(value)

  implicit val codec: Codec[VPackBoolean] = Codec(encoder, decoder)

  val dCodec: Codec[VPackBoolean] = discriminated[VPackBoolean].by(byte)
    .subcaseP(0x19) { case v @ VPackBoolean(false) => v } (provide(false).as)
    .subcaseP(0x1a) { case v @ VPackBoolean(true) => v } (provide(true).as)
}

case class VPackDouble(value: Double) extends VPackValue

object VPackDouble {
  implicit val codec: Codec[VPackDouble] = { constant(0x1b) ~> doubleL }.as
}

case class VPackDate(value: Long) extends VPackValue

object VPackDate {
  implicit val codec: Codec[VPackDate] = { constant(0x1c) ~> int64L }.as
}

case object VPackExternal extends VPackValue {
  implicit val codec: Codec[VPackExternal.type] = constant(0x1d) ~> provide(VPackExternal)
}

case object VPackMinKey extends VPackValue {
  implicit val codec: Codec[VPackMinKey.type] = constant(0x1e) ~> provide(VPackMinKey)
}

case object VPackMaxKey extends VPackValue {
  implicit val codec: Codec[VPackMaxKey.type] = constant(0x1f) ~> provide(VPackMaxKey)
}

case class VPackLong(value: Long) extends VPackValue

object VPackLong {

  val dCodec: Codec[VPackLong] = {
    val base = discriminated[VPackLong].by(uint8)

    val smalls = (-6 to 9).foldLeft(base) { (codec, small) =>
      val tag = small + (if (small > 0) 0x30 else 0x40)
      codec.subcaseP(tag) { case v @ VPackLong(`small`) => v } (provide(small.toLong).as)
    }

    val signeds = (0 to 6).foldLeft(smalls) { (codec, size) =>
      val bits = 8 * (size + 1)
      codec.subcaseP(0x20 + size) {
        case v @ VPackLong(s) if s < 0 && s >= -(1L << (bits - 1)) => v
      } (longL(bits).as)
    }.subcaseP(0x27) { case v @ VPackLong(s) if s < 0 => v } (int64L.as)

    val unsigneds = (0 to 6).foldLeft(signeds) { (codec, size) =>
      val bits = 8 * (size + 1)
      codec.subcaseP(0x28) {
        case v @ VPackLong(u) if u > 0 && u < (1L << bits) => v
      } (ulongL(bits).as)
    }.subcaseP(0x2f) { case v @ VPackLong(u) if u > 0 => v } (int64L.as)

    unsigneds
  }

  val encoder: Encoder[VPackLong] = Encoder { _.value match {
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
    case s if s < 0 && s >= -(1L << 7) => longL(8).encode(s).map { BitVector(0x20) ++ _ }
    case s if s < 0 && s >= -(1L << 15) => longL(16).encode(s).map { BitVector(0x21) ++ _ }
    case s if s < 0 && s >= -(1L << 23) => longL(24).encode(s).map { BitVector(0x22) ++ _ }
    case s if s < 0 && s >= -(1L << 31) => longL(32).encode(s).map { BitVector(0x23) ++ _ }
    case s if s < 0 && s >= -(1L << 39) => longL(40).encode(s).map { BitVector(0x24) ++ _ }
    case s if s < 0 && s >= -(1L << 47) => longL(48).encode(s).map { BitVector(0x25) ++ _ }
    case s if s < 0 && s >= -(1L << 55) => longL(56).encode(s).map { BitVector(0x26) ++ _ }
    case s if s < 0 => longL(64).encode(s).map { BitVector(0x27) ++ _ }
    // positive as unsigned
    case u if u > 0 && u < (1L << 8)  => ulongL(8).encode(u).map { BitVector(0x28) ++ _ }
    case u if u > 0 && u < (1L << 16) => ulongL(16).encode(u).map { BitVector(0x29) ++ _ }
    case u if u > 0 && u < (1L << 24) => ulongL(24).encode(u).map { BitVector(0x2a) ++ _ }
    case u if u > 0 && u < (1L << 32) => ulongL(32).encode(u).map { BitVector(0x2b) ++ _ }
    case u if u > 0 && u < (1L << 40) => ulongL(40).encode(u).map { BitVector(0x2c) ++ _ }
    case u if u > 0 && u < (1L << 48) => ulongL(48).encode(u).map { BitVector(0x2d) ++ _ }
    case u if u > 0 && u < (1L << 56) => ulongL(56).encode(u).map { BitVector(0x2e) ++ _ }
    case u if u > 0 => longL(64).encode(u).map { BitVector(0x2f) ++ _ }
  } }

  val decoder: Decoder[VPackLong] = for {
    head  <- uint8L
    value <- head match {
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
      case _ => fail(Err("not a vpack number"))
    }
  } yield VPackLong(value)

  val fCodec: Codec[VPackLong] = Codec(encoder, decoder)

  implicit val codec = fCodec
}

case class VPackString(value: String) extends VPackValue

object VPackString {

  val encoder: Encoder[VPackString] = Encoder { s =>
    for {
      bs    <- utf8.encode(s.value)
      bytes = bs.bytes
      len   = bytes.size
      head  = if (len > 126) BitVector(0xbf) ++ BitVector.fromLong(len, ordering = ByteOrdering.LittleEndian)
              else BitVector(0x40 + len)
    } yield head ++ bs
  }

  val decoder: Decoder[VPackString] = for {
    head  <- uint8L
    len   <- if (head == 0xbf) int64L
             else if (head >= 0x40 && head < 0xbf) provide[Long](head - 0x40)
             else fail(Err("not a string"))
    str   <- fixedSizeBytes(len, utf8)
  } yield VPackString(str)

  implicit val codec: Codec[VPackString] = Codec(encoder, decoder)

}

case class VPackBinary(value: ByteVector) extends VPackValue {
  def lengthSize: Int = codecs.ulongLength(value.size)
}

object VPackBinary {
  implicit val codec: Codec[VPackBinary] = {
    between(uint8L, 0xc0, 0xc7) >>~ (delta =>
      variableSizeBytesLong(ulongL((delta + 1) * 8), bytes)
    )
  }.xmap[VPackBinary](
    s => VPackBinary(s._2),
    p => (p.lengthSize - 1, p.value)
  )
}

object VPackValue {

  implicit val codec: Codec[VPackValue] = lazily { Codec.coproduct[VPackValue].choice }

  val vpBool: Codec[Boolean] = VPackBoolean.codec.as
  val vpString: Codec[String] = VPackString.codec.as //.xmap(_.value, VPackString.apply)
  val vpDouble: Codec[Double] = VPackDouble.codec.as // xmap(_.value, VPackDouble.apply)
  val vpInstant: Codec[Instant] = VPackDate.codec.xmap(d => Instant.ofEpochMilli(d.value), t => VPackDate(t.toEpochMilli))

  val vpInt: Codec[Int] = VPackLong.codec.narrow({
    case VPackLong(value) if value.isValidInt => Attempt.successful(value.toInt)
    case VPackLong(value) => Attempt.failure(Err(s"Long to Int failure for value $value"))
  }, v => VPackLong(v.toLong))

}
