package avokka.velocypack

import java.time.Instant

import avokka.velocypack.codecs.between
import scodec._
import scodec.bits._
import scodec.codecs._
import spire.math.ULong

sealed trait VPackValue {
}

case object VPackReserved1 extends VPackValue {
  val byte = hex"15"
  implicit val codec: Codec[VPackReserved1.type] = constant(byte) ~> provide(VPackReserved1)
}

case object VPackReserved2 extends VPackValue {
  val byte = hex"16"
  implicit val codec: Codec[VPackReserved2.type] = constant(byte) ~> provide(VPackReserved2)
}

case object VPackIllegal extends VPackValue {
  val byte = hex"17"
  implicit val codec: Codec[VPackIllegal.type] = constant(byte) ~> provide(VPackIllegal)
}

case object VPackNull extends VPackValue {
  val byte = hex"18"
  implicit val codec: Codec[VPackNull.type] = constant(byte) ~> provide(VPackNull)
}

object VPackBoolean {

  val codec: Codec[Boolean] = uint8L.narrow({
    case 0x19 => Attempt.successful(false)
    case 0x1a => Attempt.successful(true)
    case _    => Attempt.failure(Err("not a vpack boolean"))
  }, if (_) 0x1a else 0x19)

}

object VPackDouble {
  val codec: Codec[Double] = constant(0x1b) ~> doubleL
}

object VPackInstant {
  val codec: Codec[Instant] = constant(0x1c) ~> int64L.xmap(Instant.ofEpochMilli,_.toEpochMilli)
}

case object VPackExternal extends VPackValue {
  val byte = hex"1d"
  implicit val codec: Codec[VPackExternal.type] = constant(byte) ~> provide(VPackExternal)
}

case object VPackMinKey extends VPackValue {
  val byte = hex"1e"
  implicit val codec: Codec[VPackMinKey.type] = constant(byte) ~> provide(VPackMinKey)
}

case object VPackMaxKey extends VPackValue {
  val byte = hex"1f"
  implicit val codec: Codec[VPackMaxKey.type] = constant(byte) ~> provide(VPackMaxKey)
}

case class VPackInt(value: Long) extends VPackValue {
  def lengthSize: Int = codecs.bytesRequire(value, true)
}

object VPackInt {

  val encoder: Encoder[VPackInt] = Encoder { i =>
    val len = i.lengthSize
    (constant(0x1f + len) ~> longL(len * 8)).encode(i.value)
  }

  val decoders: Seq[Codec[Long]] = Range.inclusive(1, 8).map { len =>
    constant(0x1f + len) ~> longL(len * 8)
  }

  val decoder: Decoder[VPackInt] = Decoder.choiceDecoder(decoders: _*).map(VPackInt.apply)

  implicit val codec: Codec[VPackInt] = Codec(encoder, decoder)
}

case class VPackUInt(value: ULong) extends VPackValue {
  def lengthSize: Int = codecs.bytesRequire(value.toLong, false)
}

object VPackUInt {

  val decoders: Decoder[VPackUInt] = Decoder.choiceDecoder(
    constant(hex"28") ~> ulongL(8),
    constant(hex"29") ~> ulongL(16),
    constant(hex"2a") ~> ulongL(24),
    constant(hex"2b") ~> ulongL(32),
    constant(hex"2c") ~> ulongL(40),
    constant(hex"2d") ~> ulongL(48),
    constant(hex"2e") ~> ulongL(56),
    constant(hex"2f") ~> longL(64),
  ).map(l => VPackUInt(ULong(l)))

  val encoder: Encoder[VPackUInt] = Encoder { i =>
    val len = i.lengthSize
    (constant(0x27 + len) ~> ulongL(len * 8)).encode(i.value.toLong)
  }

  implicit val codec: Codec[VPackUInt] = Codec(encoder, decoders)
}

case class VPackSmallInt(value: Int) extends VPackValue {
  require((-6 <= value) && (value <= 9))
}

object VPackSmallInt {

  val encoder: Encoder[Int] = Encoder( b =>
    if (-6 <= b && b <= 9) Attempt.successful(BitVector(b + (if(b < 0) 0x40 else 0x30)))
    else Attempt.failure(Err("not a vpack small int"))
  )

  val decoder: Decoder[VPackSmallInt] = between(uint8L, 0x30, 0x3f).map { s =>
    VPackSmallInt(if (s < 10) s else s - 16)
  }

  implicit val codec: Codec[VPackSmallInt] = Codec(encoder.contramap[VPackSmallInt](_.value), decoder)
}

object VPackString {

  val encoder: Encoder[String] = Encoder { s =>
    for {
      bs    <- utf8.encode(s)
      bytes = bs.bytes
      len = bytes.size
      head  <- if (len > 126) int64L.encode(len).map(BitVector(0xbf) ++ _)
               else Attempt.successful(BitVector(0x40 + len))
    } yield head ++ bs
  }

  val decoder: Decoder[String] = Decoder { b =>
    for {
      head  <- uint8L.decode(b)
      len   <- if (head.value >= 0x40 && head.value < 0xbf) Attempt.successful(head.map(_.toLong - 0x40))
               else if (head.value == 0xbf) int64L.decode(head.remainder)
               else Attempt.failure(Err("not a string"))
      str   <- fixedSizeBytes(len.value, utf8).decode(len.remainder)
    } yield str
  }

  val codec: Codec[String] = Codec(encoder, decoder)

}

case class VPackBinary(value: ByteVector) extends VPackValue {
  def lengthSize: Int = codecs.bytesRequire(value.size, false)
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

  val vpBool: Codec[Boolean] = VPackBoolean.codec
  val vpString: Codec[String] = VPackString.codec
  val vpDouble: Codec[Double] = VPackDouble.codec

}
