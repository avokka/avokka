package avokka.velocypack

import java.time.Instant

import avokka.velocypack.codecs.between
import scodec._
import scodec.bits._
import scodec.codecs._

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

sealed trait VPackBoolean extends VPackValue {
  def value: Boolean
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

/*
case class VPackInt(value: Int) extends VPackValue

object VPackInt {
  val byte = hex"20"

//  val c1: Codec[Int] = { constant(hex"20") :: int8L }.dropUnits.as

  implicit val codec: Codec[VPackInt] = {
    between(uint8L, 0x20, 0x27) >>~ (size => fixedSizeBytes(size + 1, utf8))
    }.xmap[VPackInt](
    s => VPackStringShort(s._2),
    p => (p.value.length, p.value)
  )
}
 */

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
    between(uint8L, 0x40, 0xbe) >>~ (size => fixedSizeBytes(size, utf8))
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
  def lengthSize: Int = {
    val length = value.size
    if      (length > 0xffffffffffffffL) 8
    else if (length > 0xffffffffffffL) 7
    else if (length > 0xffffffffffL) 6
    else if (length > 0xffffffffL) 5
    else if (length > 0xffffffL) 4
    else if (length > 0xffffL) 3
    else if (length > 0xffL) 2
    else 1
  }
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


  def main(args: Array[String]): Unit = {
    for {
      e <- codec.encode(VPackBinary(hex"aabb"))
      d <- codec.decode(e)
    } yield println(e, d)
  }
}
