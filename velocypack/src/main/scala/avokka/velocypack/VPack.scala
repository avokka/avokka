package avokka.velocypack

import scodec.bits.ByteVector

import scala.math.ScalaNumericAnyConversions

/**
  * Velocypack value
  */
sealed trait VPack extends Any with Product with Serializable {

  /**
   * decodes vpack value to T
   * @param decoder implicit decoder
   * @tparam T decoded type
   * @return either error or T value
   */
  def as[T](implicit decoder: VPackDecoder[T]): Result[T] = decoder(this)

  /**
   * the value is empty (none, null, "", [], {})
   * @return
   */
  def isEmpty: Boolean

  /**
    * type name
    * @return
    */
  def name: String
}

object VPack {

  /**
   * indicates absence of any type and value, this is not allowed in VPack values
   * encodes from Unit and serializes to empty bitvector
   */
  case object VNone extends VPack {
    override val isEmpty: Boolean = true
    override val name: String = "none"
  }

  /**
   * can be used to indicate a value that is illegal in the embedding application
   */
  case object VIllegal extends VPack {
    override val isEmpty: Boolean = false
    override val name: String = "illegal"
  }

  /**
   * null
   */
  case object VNull extends VPack {
    override val isEmpty: Boolean = true
    override val name: String = "null"
  }

  /**
   * boolean
   */
  sealed trait VBoolean extends VPack {
    override val isEmpty: Boolean = false
    def value: Boolean
  }

  case object VFalse extends VBoolean {
    override val value = false
    override val name: String = "false"
  }
  case object VTrue extends VBoolean {
    override val value = true
    override val name: String = "true"
  }

  /**
   * double
   * @param value value
   */
  final case class VDouble(value: Double) extends AnyVal with VPack {
    override def isEmpty: Boolean = false
    override def name: String = "double"
  }

  /**
   * universal UTC-time measured in milliseconds since the epoch
   * @param value milliseconds
   */
  final case class VDate(value: Long) extends AnyVal with VPack {
    override def isEmpty: Boolean = false
    override def name: String = "date"
  }

  /**
   * artifical minimal key
   */
  case object VMinKey extends VPack {
    override val isEmpty: Boolean = false
    override val name: String = "min-key"
  }

  /**
   * artifical maximal key
   */
  case object VMaxKey extends VPack {
    override val isEmpty: Boolean = false
    override val name: String = "max-key"
  }

  /**
   * small values -6 to 9
   * @param value value
   */
  final case class VSmallint(value: Byte) extends AnyVal with VPack {
    override def isEmpty: Boolean = false
    override def name: String = "smallint"
  }

  object VSmallint {
    def isValid[T](arg: T)(implicit num: Numeric[T]): Boolean = {
      num.lt(arg, num.fromInt(10)) && num.gt(arg, num.fromInt(-7))
    }

    def isValidByte(n: ScalaNumericAnyConversions): Boolean = n.isValidByte && isValid(n.toByte)
  }

  /**
   * integer
   * @param value value
   */
  final case class VLong(value: Long) extends AnyVal with VPack {
    override def isEmpty: Boolean = false
    override def name: String = "int"
  }

  /**
   * string
   * @param value value
   */
  final case class VString(value: String) extends AnyVal with VPack {
    override def isEmpty: Boolean = value.isEmpty
    override def name: String = "string"
  }

  /**
   * binary data
   * @param value value
   */
  final case class VBinary(value: ByteVector) extends AnyVal with VPack {
    override def isEmpty: Boolean = value.isEmpty
    override def name: String = "binary"
  }

  /**
   * array
   * @param values values
   */
  final case class VArray(values: Vector[VPack]) extends AnyVal with VPack {
    override def isEmpty: Boolean = values.isEmpty
    override def name: String = "array"
  }
  object VArray {
    def apply(values: VPack*): VArray = VArray(values.toVector)
    val empty: VArray = VArray()
  }

  /**
   * object
   * @param values values
   */
  final case class VObject(values: Map[String, VPack]) extends AnyVal with VPack {
    override def isEmpty: Boolean = values.isEmpty
    override def name: String = "object"
    def updated[T: VPackEncoder](key: String, value: T): VObject = copy(values = values.updated(key, value.toVPack))
    def filter(p: ((String, VPack)) => Boolean): VObject = copy(values = values.filter(p))
  }
  object VObject {
    def apply(values: (String, VPack)*): VObject = VObject(values.toMap)
    val empty: VObject = VObject()
  }
}
