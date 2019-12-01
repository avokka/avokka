package avokka.velocypack

import scodec.bits._

sealed trait VPackValue

case object VPackIllegal extends VPackValue

case object VPackNull extends VPackValue

case class VPackBoolean(value: Boolean) extends VPackValue

case class VPackDouble(value: Double) extends VPackValue

case class VPackDate(value: Long) extends VPackValue

case object VPackMinKey extends VPackValue
case object VPackMaxKey extends VPackValue

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
