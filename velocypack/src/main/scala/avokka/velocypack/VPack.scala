package avokka.velocypack

import cats.data.Chain
import scodec.bits.ByteVector

sealed trait VPack

object VPack {

  case object VIllegal extends VPack

  case object VNull extends VPack

  case class VBoolean(value: Boolean) extends VPack
  val VFalse: VPack = VBoolean(false)
  val VTrue: VPack = VBoolean(true)

  case class VDouble(value: Double) extends VPack

  case class VDate(value: Long) extends VPack

  case object VMinKey extends VPack
  case object VMaxKey extends VPack

  case class VSmallint(value: Byte) extends VPack {
    require(-7 < value && value < 10)
  }

  object VSmallint {

    def fromNumeric[T](arg: T)(implicit num: Numeric[T]): Option[VSmallint] = {
      if (num.lt(arg, num.fromInt(10)) && num.gt(arg, num.fromInt(-7)))
        Some(VSmallint(num.toInt(arg).toByte))
      else None
    }

    object From {
      def unapply(i: Int): Option[VSmallint] = fromNumeric(i)
      def unapply(s: Short): Option[VSmallint] = fromNumeric(s)
      def unapply(l: Long): Option[VSmallint] = fromNumeric(l)
      def unapply(d: Double): Option[VSmallint] = if (d.isWhole()) fromNumeric(d) else None
    }
  }

  case class VLong(value: Long) extends VPack

  object VLong {
    object From {
      def unapply(i: Int): Option[VLong] = Some(VLong(i.toLong))
      def unapply(s: Short): Option[VLong] = Some(VLong(s.toLong))
      def unapply(d: Double): Option[VLong] = if (d.isWhole()) Some(VLong(d.toLong)) else None
    }
  }

  case class VString(value: String) extends VPack

  case class VBinary(value: ByteVector) extends VPack

  case class VArray(values: Chain[VPack]) extends VPack
  object VArray {
    def apply(values: VPack*): VArray = VArray(Chain.fromSeq(values))
  }
  val VArrayEmpty: VPack = VArray()

  case class VObject(values: Map[String, VPack]) extends VPack
  val VObjectEmpty: VPack = VObject(Map.empty)

}