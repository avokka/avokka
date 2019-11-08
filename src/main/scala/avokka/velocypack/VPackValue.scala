package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._

sealed trait VPackValue {
}

sealed trait VPackString extends VPackValue {
  def value: String
}

object VPackString {
  val r = Range.inclusive(0x40, 0x45)

  r.contains(0x41)
}

case class VPackStringShort(value: String) extends VPackString {

}

object VPackStringShort {
  def byteType: Byte = 0x40
  implicit val codec: Codec[VPackStringShort] = { constant(byteType) :: utf8 }.dropUnits.as[VPackStringShort]
}

case class VPackStringLong(value: String) extends VPackString {
}

object VPackStringLong {
  val byte: Byte = 0xbf.toByte
  implicit val codec: Codec[VPackStringLong] = { constant(byte) :: utf8_32L }.dropUnits.as[VPackStringLong]
}

case object VPackNull extends VPackValue {
  val byte: Byte = 0x18
  implicit val codec: Codec[VPackNull.type] = constant(byte) ~> provide(VPackNull)
}

sealed trait VPackBoolean extends VPackValue {
  def value: Boolean
}

case object VPackFalse extends VPackBoolean {
  val byte: Byte = 0x19
  override val value = false
  implicit val codec: Codec[VPackFalse.type] = constant(byte) ~> provide(VPackFalse)
}

case object VPackTrue extends VPackBoolean {
  val byte: Byte = 0x1a
  override val value = true
  implicit val codec: Codec[VPackTrue.type] = constant(byte) ~> provide(VPackTrue)
}

object VPackValue {
  implicit val codec: Codec[VPackValue] = lazily { Codec.coproduct[VPackValue].choice }

}

/*
case class VPackNull()

object VPackNull {
  implicit val codec: Codec[VPackNull] = {
    constant(hex"18")
  }.as[VPackNull]
}

case class VPackString
(
  value: String
) extends VPackValue

object VPackString {
  implicit val codec: Codec[VPackString] = {
    ("head" | constant(hex""))
  }
}
*/