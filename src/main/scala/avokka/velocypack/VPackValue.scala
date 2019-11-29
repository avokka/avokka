package avokka.velocypack

import scodec._
import scodec.bits._
import scodec.codecs._

sealed trait VPackValue

case object VPackReserved1 extends VPackValue {
  implicit val codec: Codec[VPackReserved1.type] = constant(0x15) ~> provide(VPackReserved1)
}

case object VPackReserved2 extends VPackValue {
  implicit val codec: Codec[VPackReserved2.type] = constant(0x16) ~> provide(VPackReserved2)
}

case object VPackIllegal extends VPackValue {
  implicit val codec: Codec[VPackIllegal.type] = constant(0x17) ~> provide(VPackIllegal)
}

case object VPackNull extends VPackValue
case class VPackBoolean(value: Boolean) extends VPackValue
case class VPackDouble(value: Double) extends VPackValue
case class VPackDate(value: Long) extends VPackValue
case object VPackExternal extends VPackValue {
  implicit val codec: Codec[VPackExternal.type] = constant(0x1d) ~> provide(VPackExternal)
}

case object VPackMinKey extends VPackValue {
  implicit val codec: Codec[VPackMinKey.type] = constant(0x1e) ~> provide(VPackMinKey)
}

case object VPackMaxKey extends VPackValue {
  implicit val codec: Codec[VPackMaxKey.type] = constant(0x1f) ~> provide(VPackMaxKey)
}

case class VPackSmallint(value: Byte) extends VPackValue {
  require(-7 < value && value < 10)
}

object VPackSmallint {

  def fromNumeric[T](arg: T)(implicit num: Numeric[T]): Option[VPackSmallint] = {
    if (num.lt(arg, num.fromInt(10)) && num.gt(arg, num.fromInt(-7)))
      Some(VPackSmallint(num.toInt(arg).toByte))
    else None
  }

  def unapply(i: Int): Option[VPackSmallint] = fromNumeric(i)
  def unapply(l: Long): Option[VPackSmallint] = fromNumeric(l)
  def unapply(d: Double): Option[VPackSmallint] = if (d.isWhole()) fromNumeric(d) else None
}

case class VPackLong(value: Long) extends VPackValue

object VPackLong {
  def unapply(i: Int): Option[VPackLong] = Some(VPackLong(i.toLong))
  def unapply(d: Double): Option[VPackLong] = if (d.isWhole()) Some(VPackLong(d.toLong)) else None
}

case class VPackString(value: String) extends VPackValue

case class VPackBinary(value: ByteVector) extends VPackValue

case class VPackArray(values: Seq[BitVector]) extends VPackValue

case class VPackObject(values: Map[String, BitVector]) extends VPackValue

object VPackValue {
//  import codecs._
//  implicit val codec: Codec[VPackValue] = Codec.coproduct[VPackReserved1.type :+: VPackNull.type  :+: CNil].choice

}
