package avokka.velocypack

import java.time.Instant

import avokka.velocypack.codecs.between
import com.arangodb.velocypack.{VPack, VPackSlice}
import scodec._
import scodec.bits._
import scodec.codecs._
import spire.math.ULong

sealed trait VPackValue {
}

case object VPackNone extends VPackValue {
  val byte = hex"00"
  implicit val codec: Codec[VPackNone.type] = constant(byte) ~> provide(VPackNone)
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

sealed trait VPackBoolean extends VPackValue {
  def value: Boolean
}

object VPackBoolean {
  def apply(b: Boolean): VPackBoolean = if (b) VPackTrue else VPackFalse
  implicit val codec: Codec[VPackBoolean] = lazily { Codec.coproduct[VPackBoolean].choice }
}

case object VPackFalse extends VPackBoolean {
  val byte = hex"19"
  override val value = false
  implicit val codec: Codec[VPackFalse.type] = constant(byte) ~> provide(VPackFalse)
}

case object VPackTrue extends VPackBoolean {
  val byte = hex"1a"
  override val value = true
  implicit val codec: Codec[VPackTrue.type] = constant(byte) ~> provide(VPackTrue)
}

case class VPackDouble(value: Double) extends VPackValue

object VPackDouble {
  val byte = hex"1b"
  implicit val codec: Codec[VPackDouble] = { constant(byte) :: doubleL }.dropUnits.as
}

case class VPackInstant(value: Instant) extends VPackValue

object VPackInstant {
  val byte = hex"1c"
  val instantCodec: Codec[Instant] = int64L.xmap(Instant.ofEpochMilli,_.toEpochMilli)
  implicit val codec: Codec[VPackInstant] = { constant(byte) :: instantCodec }.dropUnits.as
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
  val byte = hex"20"

  val encoder: Encoder[VPackInt] = Encoder { i =>
    val len = i.lengthSize
    (constant(0x1f + len) ~> longL(len * 8)).encode(i.value)
  }

  val decoders: Seq[Codec[Long]] = Range.inclusive(1, 8).map { len =>
    constant(0x1f + len) ~> longL(len * 8)
  }

  val decoder: Decoder[VPackInt] = Decoder.choiceDecoder(decoders: _*).map(VPackInt.apply)

  implicit val codec: Codec[VPackInt] = Codec(encoder, decoder)

  /*
  val codecOld: Codec[VPackInt] = {
    between(uint8L, 0x20, 0x27) >>~ (delta => longL((delta + 1) * 8))
  }.xmap[VPackInt](
    s => VPackInt(s._2),
    p => (p.lengthSize - 1, p.value)
  )
   */
}

case class VPackUInt(value: ULong) extends VPackValue {
  def lengthSize: Int = codecs.bytesRequire(value.toLong, false)
}

object VPackUInt {
  val byte = hex"28"

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

  /*
  implicit val codec: Codec[VPackUInt] = {
    between(uint8L, 0x28, 0x2f) >>~ (delta => ulongL((delta + 1) * 8))
    }.xmap[VPackUInt](
    s => VPackUInt(ULong(s._2)),
    p => (p.lengthSize - 1, p.value.toLong)
  )
   */
}

case class VPackSmallInt(value: Int) extends VPackValue {
  require((-6 <= value) && (value <= 9))
}

object VPackSmallInt {
  val byte = hex"30"

  val encoder: Encoder[Byte] = Encoder( _ match {
    case 0 => Attempt.successful(hex"30".bits)
    case 1 => Attempt.successful(hex"31".bits)
    case 2 => Attempt.successful(hex"32".bits)
    case 3 => Attempt.successful(hex"33".bits)
    case 4 => Attempt.successful(hex"34".bits)
    case 5 => Attempt.successful(hex"35".bits)
    case 6 => Attempt.successful(hex"36".bits)
    case 7 => Attempt.successful(hex"37".bits)
    case 8 => Attempt.successful(hex"38".bits)
    case 9 => Attempt.successful(hex"39".bits)
    case -6 => Attempt.successful(hex"3a".bits)
    case -5 => Attempt.successful(hex"3b".bits)
    case -4 => Attempt.successful(hex"3c".bits)
    case -3 => Attempt.successful(hex"3d".bits)
    case -2 => Attempt.successful(hex"3e".bits)
    case -1 => Attempt.successful(hex"3f".bits)
    case _  => Attempt.failure(Err("not a vpack small int"))
  })

  val decoder: Decoder[VPackSmallInt] = {
    between(uint8L, 0x30, 0x3f)
  }.asDecoder.map[VPackSmallInt](
    s => VPackSmallInt(if (s < 10) s else s - 16)
  )

  implicit val codec: Codec[VPackSmallInt] = Codec(encoder.contramap[VPackSmallInt](_.value.toByte), decoder)
}



sealed trait VPackString extends VPackValue {
  def value: String
}

object VPackString {
}

case class VPackStringShort(value: String) extends VPackString {
  require(value.length <= 126, "VPackStringShort length must be <= 126")
}

object VPackStringShort {
  val byte = hex"40"

  implicit val codec: Codec[VPackStringShort] = {
    between(uint8L, 0x40, 0xbe) >>~ (delta => fixedSizeBytes(delta, utf8))
  }.xmap[VPackStringShort](
    s => VPackStringShort(s._2),
    p => (p.value.length, p.value)
  )
}

case class VPackStringLong(value: String) extends VPackString

object VPackStringLong {
  val byte = hex"bf"
  implicit val codec: Codec[VPackStringLong] = { constant(byte) :: utf8_32L }.dropUnits.as
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

  implicit val bool: Codec[Boolean] = VPackBoolean.codec.xmap(_.value, VPackBoolean.apply)

  def main(args: Array[String]): Unit = {
    val e = codec.encode(VPackSmallInt(5))
    println(e)

    val vpack = new VPack.Builder().build()
    println(vpack.deserialize(new VPackSlice(e.require.toByteArray), classOf[Int]))
    /*
    for {
      i <- -6 to 9
    } for {
      e <- codec.encode(VPackSmallInt(i))
      d <- codec.decode(e)
    } yield println(e, d)
     */
  }
}
