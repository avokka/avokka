package avokka.velocypack
package codecs

import scodec.bits.BitVector
import scodec.codecs._
import scodec.{Attempt, Codec, Decoder, Encoder, Err}

/**
  * velocypack value type
  */
private sealed trait VPackType extends Product with Serializable {

  /**
    * @return the head byte
    */
  def header: Int

  /**
    * @return the bitvector of the head
    */
  lazy val bits: BitVector = BitVector(header)

  /**
    * @return type name
    */
  def name: String
}

private object VPackType {

  /**
    * velocypack types having a length field after the head
    */
  private[codecs] sealed trait WithLength { self: VPackType =>

    /**
      * @return size in bytes of the length field
      */
    def lengthSize: Int

    /**
      * @return decoder of the value's length
      */
    def lengthDecoder: Decoder[Long]
  }

  /** array or object with data */
  private[codecs] sealed abstract class CompoundType(minByte: Int) extends VPackType with WithLength {
    override val lengthSize: Int = 1 << (header - minByte)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }

  /**
    * types which don't have value body and map to a single value
    * @param header byte head
    * @param singleton corresponding vpack value
    */
  sealed abstract class SingleByte(override val header: Int, val singleton: VPack) extends VPackType

  /** 0x00 : none - this indicates absence of any type and value, this is not allowed in VPack values */
  case object NoneType extends VPackType {
    override val header: Int = 0x00
    override val name: String = "none"
  }

  /** 0x01 : empty array */
  case object ArrayEmptyType extends SingleByte(0x01, VPack.VArray.empty) {
    override val name: String = "array(empty)"
  }

  /** 0x02-0x05 : array without index table (all subitems have the same byte length), [1,2,4,8]-byte byte length */
  final case class ArrayUnindexedType(override val header: Int) extends CompoundType(ArrayUnindexedType.minByte) {
    import ArrayUnindexedType._
    require(header >= minByte && header <= maxByte)
    override val name: String = "array(unindexed)"
  }
  object ArrayUnindexedType {
    val minByte = 0x02
    val maxByte = 0x05
  }

  /** 0x06-0x09 : array with [1,2,4,8]-byte index table offsets, bytelen and # subvals */
  final case class ArrayIndexedType(override val header: Int) extends CompoundType(ArrayIndexedType.minByte) {
    import ArrayIndexedType._
    require(header >= minByte && header <= maxByte)
    override val name: String = "array(indexed)"
  }
  object ArrayIndexedType {
    val minByte = 0x06
    val maxByte = 0x09
  }

  /** 0x0a : empty object */
  case object ObjectEmptyType extends SingleByte(0x0a, VPack.VObject.empty) {
    override val name: String = "object(empty)"
  }

  /** object with data */
  private[codecs] sealed trait ObjectType extends VPackType with WithLength

  /** 0x0b-0x0e : object with 1-byte index table offsets, sorted by attribute name, [1,2,4,8]-byte bytelen and # subvals */
  final case class ObjectSortedType(override val header: Int) extends CompoundType(ObjectSortedType.minByte) with ObjectType {
    import ObjectSortedType._
    require(header >= minByte && header <= maxByte)
    override val name: String = "object(sorted)"
  }
  object ObjectSortedType {
    val minByte = 0x0b
    val maxByte = 0x0e
  }

  /** 0x0f-0x12 : object with 1-byte index table offsets, not sorted by attribute name, [1,2,4,8]-byte bytelen and # subvals */
  final case class ObjectUnsortedType(override val header: Int) extends CompoundType(ObjectUnsortedType.minByte) with ObjectType {
    import ObjectUnsortedType._
    require(header >= minByte && header <= maxByte)
    override val name: String = "object(unsorted)"
  }
  object ObjectUnsortedType {
    val minByte = 0x0f
    val maxByte = 0x12
  }

  /** 0x13 : compact array, no index table */
  case object ArrayCompactType extends VPackType {
    override val header: Int = 0x13
    override val name: String = "array(compact)"
  }

  /** 0x14 : compact object, no index table */
  case object ObjectCompactType extends VPackType {
    override val header: Int = 0x14
    override val name: String = "object(compact)"
  }

  // 0x15-0x16 : reserved

  /** 0x17 : illegal - this type can be used to indicate a value that is illegal in the embedding application */
  case object IllegalType extends SingleByte(0x17, VPack.VIllegal) {
    override val name: String = "illegal"
  }

  /** 0x18 : null */
  case object NullType extends SingleByte(0x18, VPack.VNull) {
    override val name: String = "null"
  }

  /** 0x19 : false */
  case object FalseType extends SingleByte(0x19, VPack.VFalse) {
    override val name: String = "false"
  }

  /** 0x1a : true */
  case object TrueType extends SingleByte(0x1a, VPack.VTrue) {
    override val name: String = "true"
  }

  /** 0x1b : double IEEE-754, 8 bytes follow, stored as little endian uint64 equivalent */
  case object DoubleType extends VPackType {
    override val header: Int = 0x1b
    override val name: String = "double"
  }

  /** 0x1c : UTC-date in milliseconds since the epoch, stored as 8 byte signed int, little endian, two's complement */
  case object DateType extends VPackType {
    override val header: Int = 0x1c
    override val name: String = "date"
  }

  // 0x1d : external (only in memory): a char* pointing to the actual place in memory, where another VPack item resides,
  // not allowed in VPack values on disk or on the network

  /** 0x1e : minKey, nonsensical value that compares < than all other values */
  case object MinKeyType extends SingleByte(0x1e, VPack.VMinKey) {
    override val name: String = "min-key"
  }

  /** 0x1f : maxKey, nonsensical value that compares > than all other values */
  case object MaxKeyType extends SingleByte(0x1f, VPack.VMaxKey) {
    override val name: String = "max-key"
  }

  private[codecs] sealed abstract class IntType(minByte: Int) extends VPackType with WithLength {
    override val lengthSize: Int = header - minByte + 1
    override val lengthDecoder: Decoder[Long] = provide(0)
  }

  /** 0x20-0x27 : signed int, little endian, 1 to 8 bytes, number is V - 0x1f, two's complement */
  final case class IntSignedType(override val header: Int) extends IntType(IntSignedType.minByte) {
    import IntSignedType._
    require(header >= minByte && header <= maxByte)
    override val name: String = "int(signed)"
  }
  object IntSignedType {
    val minByte = 0x20
    val maxByte = 0x27
  }

  /** 0x28-0x2f : uint, little endian, 1 to 8 bytes, number is V - 0x27 */
  final case class IntUnsignedType(override val header: Int) extends IntType(IntUnsignedType.minByte) {
    import IntUnsignedType._
    require(header >= minByte && header <= maxByte)
    override val name: String = "int(unsigned)"
  }
  object IntUnsignedType {
    val minByte = 0x28
    val maxByte = 0x2f
  }

  /** 0x30-0x39 : small integers 0, 1, ... 9 */
  final case class SmallintPositiveType(override val header: Int) extends VPackType {
    import SmallintPositiveType._
    require(header >= minByte && header <= maxByte)
    override val name: String = "smallint(positive)"
  }
  object SmallintPositiveType {
    val minByte = 0x30
    val maxByte = 0x39
  }

  /** 0x3a-0x3f : small negative integers -6, -5, ..., -1 */
  final case class SmallintNegativeType(override val header: Int) extends VPackType {
    import SmallintNegativeType._
    require(header >= minByte && header <= maxByte)
    override val name: String = "smallint(negative)"
  }
  object SmallintNegativeType {
    val minByte = 0x3a
    val maxByte = 0x3f
    val topByte = 0x40
  }

  // string types
  private[codecs] sealed trait StringType extends VPackType with WithLength

  /**
    * 0x40-0xbe : UTF-8-string, using V - 0x40 bytes (not Unicode characters!)
    * length 0 is possible, so 0x40 is the empty string
    * maximal length is 126, note that strings here are not zero-terminated
    */
  final case class StringShortType(override val header: Int) extends StringType {
    import StringShortType._
    require(header >= minByte && header <= maxByte)
    override val lengthSize: Int = 0
    override val lengthDecoder: Decoder[Long] = provide((header - minByte).toLong)
    override val name: String = "string(short)"
  }

  object StringShortType {

    /** lower bound of head for small strings */
    val minByte = 0x40

    /** upper bound of head for small strings */
    val maxByte = 0xbe

    def fromLength(length: Int): StringShortType = StringShortType(minByte + length)
  }

  /**
    * 0xbf : long UTF-8-string, next 8 bytes are length of string in bytes (not Unicode characters)
    * as little endian unsigned integer, note that long strings are not zero-terminated and may contain zero bytes
    */
  case object StringLongType extends StringType {
    override val header: Int = 0xbf
    override val lengthSize: Int = 8
    override val lengthDecoder: Decoder[Long] = int64L
    override val name: String = "string(long)"
  }

  /** 0xc0-0xc7 : binary blob, next V - 0xbf bytes are the length of blob in bytes note that binary blobs are not zero-terminated */
  final case class BinaryType(override val header: Int) extends VPackType with WithLength {
    import BinaryType._
    require(header >= minByte && header <= maxByte)
    override val lengthSize: Int = header - minByte + 1
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize)
    override val name: String = "binary"
  }
  object BinaryType {
    val minByte = 0xc0
    val maxByte = 0xc7
    def fromLength(length: Int): BinaryType = BinaryType(minByte - 1 + length)
  }

  /**
    * decode the head byte to the velocypack type
    */
  private[codecs] val vpackTypeDecoder: Decoder[VPackType] = uint8L.emap({
    case NoneType.`header` => Attempt.failure(Err("absence of type is not allowed in values"))

    case ArrayEmptyType.`header` => Attempt.successful(ArrayEmptyType)
    case header if header >= ArrayUnindexedType.minByte && header <= ArrayUnindexedType.maxByte =>
      Attempt.successful(ArrayUnindexedType(header))
    case header if header >= ArrayIndexedType.minByte && header <= ArrayIndexedType.maxByte =>
      Attempt.successful(ArrayIndexedType(header))

    case ObjectEmptyType.`header` => Attempt.successful(ObjectEmptyType)
    case header if header >= ObjectSortedType.minByte && header <= ObjectSortedType.maxByte =>
      Attempt.successful(ObjectSortedType(header))
    case header if header >= ObjectUnsortedType.minByte && header <= ObjectUnsortedType.maxByte =>
      Attempt.successful(ObjectUnsortedType(header))

    case ArrayCompactType.`header`  => Attempt.successful(ArrayCompactType)
    case ObjectCompactType.`header` => Attempt.successful(ObjectCompactType)

    case IllegalType.`header` => Attempt.successful(IllegalType)
    case NullType.`header`    => Attempt.successful(NullType)
    case FalseType.`header`   => Attempt.successful(FalseType)
    case TrueType.`header`    => Attempt.successful(TrueType)
    case DoubleType.`header`  => Attempt.successful(DoubleType)
    case DateType.`header`    => Attempt.successful(DateType)

    case MinKeyType.`header` => Attempt.successful(MinKeyType)
    case MaxKeyType.`header` => Attempt.successful(MaxKeyType)

    case header if header >= IntSignedType.minByte && header <= IntSignedType.maxByte =>
      Attempt.successful(IntSignedType(header))
    case header if header >= IntUnsignedType.minByte && header <= IntUnsignedType.maxByte =>
      Attempt.successful(IntUnsignedType(header))
    case header if header >= SmallintPositiveType.minByte && header <= SmallintPositiveType.maxByte =>
      Attempt.successful(SmallintPositiveType(header))
    case header if header >= SmallintNegativeType.minByte && header <= SmallintNegativeType.maxByte =>
      Attempt.successful(SmallintNegativeType(header))

    case header if header >= StringShortType.minByte && header <= StringShortType.maxByte =>
      Attempt.successful(StringShortType(header))
    case StringLongType.`header` => Attempt.successful(StringLongType)

    case header if header >= BinaryType.minByte && header <= BinaryType.maxByte =>
      Attempt.successful(BinaryType(header))

    case u => Attempt.failure(Err(s"unknown header byte ${u.toHexString}"))
  })

  /**
    * encodes the type to the head byte
    */
  private[codecs] val vpackTypeEncoder: Encoder[VPackType] = Encoder(t => Attempt.successful(t.bits))

  /**
    * type codec
    */
  private[codecs] val vpackTypeCodec: Codec[VPackType] = Codec(vpackTypeEncoder, vpackTypeDecoder)
}
