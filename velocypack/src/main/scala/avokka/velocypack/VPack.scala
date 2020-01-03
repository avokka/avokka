package avokka.velocypack

import cats.data.Chain
import scodec.bits.ByteVector

/**
  * Velocypack value
  */
sealed trait VPack {

  /**
   * decodes vpack value to T
   * @param decoder implicit decoder
   * @tparam T decoded type
   * @return either error or T value
   */
  def as[T](implicit decoder: VPackDecoder[T]): Result[T] = decoder.decode(this)

  /**
   * the value is empty (none, null, "", [], {})
   * @return
   */
  def isEmpty: Boolean

}

object VPack {

  /**
   * indicates absence of any type and value, this is not allowed in VPack values
   * encodes from Unit and serializes to empty bitvector
   */
  case object VNone extends VPack {
    override val isEmpty: Boolean = true
  }

  /**
   * can be used to indicate a value that is illegal in the embedding application
   */
  case object VIllegal extends VPack {
    override val isEmpty: Boolean = false
  }

  /**
   * null
   */
  case object VNull extends VPack {
    override val isEmpty: Boolean = true
  }

  /**
   * boolean
   * @param value value
   */
  case class VBoolean(value: Boolean) extends VPack {
    override val isEmpty: Boolean = false
  }

  val VFalse: VBoolean = VBoolean(false)
  val VTrue: VBoolean = VBoolean(true)

  /**
   * double
   * @param value value
   */
  case class VDouble(value: Double) extends VPack {
    override val isEmpty: Boolean = false
  }

  /**
   * universal UTC-time measured in milliseconds since the epoch
   * @param value milliseconds
   */
  case class VDate(value: Long) extends VPack {
    override val isEmpty: Boolean = false
  }

  /**
   * artifical minimal key
   */
  case object VMinKey extends VPack {
    override val isEmpty: Boolean = false
  }

  /**
   * artifical maximal key
   */
  case object VMaxKey extends VPack {
    override val isEmpty: Boolean = false
  }

  /**
   * small values -6 to 9
   * @param value value
   */
  case class VSmallint(value: Byte) extends VPack {
    require(-7 < value && value < 10)
    override val isEmpty: Boolean = false
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

  /**
   * integer
   * @param value value
   */
  case class VLong(value: Long) extends VPack {
    override val isEmpty: Boolean = false
  }

  object VLong {
    object From {
      def unapply(i: Int): Option[VLong] = Some(VLong(i.toLong))
      def unapply(s: Short): Option[VLong] = Some(VLong(s.toLong))
      def unapply(d: Double): Option[VLong] = if (d.isWhole()) Some(VLong(d.toLong)) else None
    }
  }

  /**
   * string
   * @param value value
   */
  case class VString(value: String) extends VPack {
    override def isEmpty: Boolean = value.isEmpty
  }

  /**
   * binary data
   * @param value value
   */
  case class VBinary(value: ByteVector) extends VPack {
    override def isEmpty: Boolean = value.isEmpty
  }

  /**
   * array
   * @param values values
   */
  case class VArray(values: Chain[VPack]) extends VPack {
    override def isEmpty: Boolean = values.isEmpty
  }
  object VArray {
    def apply(values: VPack*): VArray = VArray(Chain.fromSeq(values))
    val empty: VArray = VArray()
  }

  /**
   * object
   * @param values values
   */
  case class VObject(values: Map[String, VPack]) extends VPack {
    override def isEmpty: Boolean = values.isEmpty
    def updated[T: VPackEncoder](key: String, value: T): VObject = copy(values = values.updated(key, value.toVPack))
    def filter(p: ((String, VPack)) => Boolean): VObject = copy(values = values.filter(p))
  }
  object VObject {
    def apply(values: (String, VPack)*): VObject = VObject(values.toMap)
    val empty: VObject = VObject()
  }
}
