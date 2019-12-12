package avokka.velocypack.codecs

import avokka.velocypack.VPack
import scodec.bits.BitVector
import scodec.codecs._
import scodec.{Attempt, Codec, Decoder, Encoder, Err}
import scodec.interop.cats._
import cats.implicits._

/**
 * velocypack value type
 */
trait VPackType {
  /**
   * @return the head byte
   */
  def head: Int

  /**
   * @return the bitvector of the head
   */
  lazy val bits: BitVector = BitVector(head)
}

object VPackType {
  import VPack._

  /**
   * velocypack types having a length field after the head
   */
  trait WithLength { self: VPackType =>
    /**
     * @return size in bytes of the length field
     */
    def lengthSize: Int

    /**
     * @return decoder of the value's length
     */
    def lengthDecoder: Decoder[Long]
  }

  /**
   * types which don't have value body and map to a single value
   * @param head byte head
   * @param singleton corresponding vpack value
   */
  abstract class SingleByte(override val head: Int, val singleton: VPack) extends VPackType

  /** 0x00 : none - this indicates absence of any type and value, this is not allowed in VPack values */
  case object NoneType extends VPackType { override val head: Int = 0x00 }

  /** 0x01 : empty array */
  case object ArrayEmptyType extends SingleByte(0x01, VArrayEmpty)

  /** 0x02-0x05 : array without index table (all subitems have the same byte length), [1,2,4,8]-byte byte length */
  case class ArrayUnindexedType(override val head: Int) extends VPackType with WithLength {
    import ArrayUnindexedType._
    require(head >= minByte && head <= maxByte)
    override val lengthSize: Int = 1 << (head - minByte)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }
  object ArrayUnindexedType {
    val minByte = 0x02
    val maxByte = 0x05
  }

  /** 0x06-0x09 : array with [1,2,4,8]-byte index table offsets, bytelen and # subvals */
  case class ArrayIndexedType(override val head: Int) extends VPackType with WithLength {
    import ArrayIndexedType._
    require(head >= minByte && head <= maxByte)
    override val lengthSize: Int = 1 << (head - minByte)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }
  object ArrayIndexedType {
    val minByte = 0x06
    val maxByte = 0x09
  }

  /** 0x0a : empty object */
  case object ObjectEmptyType extends SingleByte(0x0a, VObjectEmpty)

  /** object with data */
  trait ObjectType extends VPackType with WithLength

  /** 0x0b-0x0e : object with 1-byte index table offsets, sorted by attribute name, [1,2,4,8]-byte bytelen and # subvals */
  case class ObjectSortedType(override val head: Int) extends ObjectType {
    import ObjectSortedType._
    require(head >= minByte && head <= maxByte)
    override val lengthSize: Int = 1 << (head - minByte)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }
  object ObjectSortedType {
    val minByte = 0x0b
    val maxByte = 0x0e
  }

  /** 0x0f-0x12 : object with 1-byte index table offsets, not sorted by attribute name, [1,2,4,8]-byte bytelen and # subvals */
  case class ObjectUnsortedType(override val head: Int) extends ObjectType {
    import ObjectUnsortedType._
    require(head >= minByte && head <= maxByte)
    override val lengthSize: Int = 1 << (head - minByte)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }
  object ObjectUnsortedType {
    val minByte = 0x0f
    val maxByte = 0x12
  }

  /** 0x13 : compact array, no index table */
  case object ArrayCompactType extends VPackType {
    override val head: Int = 0x13
  }

  /** 0x14 : compact object, no index table */
  case object ObjectCompactType extends VPackType {
    override val head: Int = 0x14
  }

  // 0x15-0x16 : reserved

  /** 0x17 : illegal - this type can be used to indicate a value that is illegal in the embedding application */
  case object IllegalType extends SingleByte(0x17, VIllegal)

  /** 0x18 : null */
  case object NullType extends SingleByte(0x18, VNull)

  /** 0x19 : false */
  case object FalseType extends SingleByte(0x19, VFalse)

  /** 0x1a : true */
  case object TrueType extends SingleByte(0x1a, VTrue)

  /** 0x1b : double IEEE-754, 8 bytes follow, stored as little endian uint64 equivalent */
  case object DoubleType extends VPackType {
    override val head: Int = 0x1b
  }

  /** 0x1c : UTC-date in milliseconds since the epoch, stored as 8 byte signed int, little endian, two's complement */
  case object DateType extends VPackType {
    override val head: Int = 0x1c
  }

  // 0x1d : external (only in memory): a char* pointing to the actual place in memory, where another VPack item resides,
  // not allowed in VPack values on disk or on the network

  /** 0x1e : minKey, nonsensical value that compares < than all other values */
  case object MinKeyType extends SingleByte(0x1e, VMinKey)

  /** 0x1f : maxKey, nonsensical value that compares > than all other values */
  case object MaxKeyType extends SingleByte(0x1f, VMaxKey)

  /** 0x20-0x27 : signed int, little endian, 1 to 8 bytes, number is V - 0x1f, two's complement */
  case class IntSignedType(override val head: Int) extends VPackType with WithLength {
    require(head >= 0x20 && head <= 0x27)
    override val lengthSize: Int = head - 0x20 + 1
    override val lengthDecoder: Decoder[Long] = provide(0)
  }

  /** 0x28-0x2f : uint, little endian, 1 to 8 bytes, number is V - 0x27 */
  case class IntUnsignedType(override val head: Int) extends VPackType with WithLength {
    require(head >= 0x28 && head <= 0x2f)
    override val lengthSize: Int = head - 0x28 + 1
    override val lengthDecoder: Decoder[Long] = provide(0)
  }

  /** 0x30-0x39 : small integers 0, 1, ... 9 */
  case class SmallintPositiveType(override val head: Int) extends VPackType {
    import SmallintPositiveType._
    require(head >= minByte && head <= maxByte)
  }
  object SmallintPositiveType {
    val minByte = 0x30
    val maxByte = 0x39
  }

  /** 0x3a-0x3f : small negative integers -6, -5, ..., -1 */
  case class SmallintNegativeType(override val head: Int) extends VPackType {
    import SmallintNegativeType._
    require(head >= minByte && head <= maxByte)
  }
  object SmallintNegativeType {
    val minByte = 0x3a
    val maxByte = 0x3f
    val topByte = 0x40
  }

  // string types
  trait StringType extends VPackType with WithLength

  /**
   * 0x40-0xbe : UTF-8-string, using V - 0x40 bytes (not Unicode characters!)
   * length 0 is possible, so 0x40 is the empty string
   * maximal length is 126, note that strings here are not zero-terminated
   */
  case class StringShortType(override val head: Int) extends StringType {
    import StringShortType._
    require(head >= minByte && head <= maxByte)
    override val lengthSize: Int = 0
    override val lengthDecoder: Decoder[Long] = provide(head - minByte)
  }

  object StringShortType {
    /** lower bound of head for small strings */
    val minByte = 0x40
    /** upper bound of head for small strings */
    val maxByte  = 0xbe

    def fromLength(length: Int): StringShortType = StringShortType(minByte + length)
  }

  /**
   * 0xbf : long UTF-8-string, next 8 bytes are length of string in bytes (not Unicode characters)
   * as little endian unsigned integer, note that long strings are not zero-terminated and may contain zero bytes
   */
  case object StringLongType extends StringType {
    override val head: Int = 0xbf
    override val lengthSize: Int = 8
    override val lengthDecoder: Decoder[Long] = int64L
  }

  /** 0xc0-0xc7 : binary blob, next V - 0xbf bytes are the length of blob in bytes note that binary blobs are not zero-terminated */
  case class BinaryType(override val head: Int) extends VPackType with WithLength {
    import BinaryType._
    require(head >= minByte && head <= maxByte)
    override val lengthSize: Int = head - minByte + 1
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize)
  }
  object BinaryType {
    val minByte = 0xc0
    val maxByte = 0xc7
    def fromLength(length: Int): BinaryType = BinaryType(minByte - 1 + length)
  }

  /**
   * decode the head byte to the velocypack type
   */
  val vpackTypeDecoder: Decoder[VPackType] = uint8L.emap({
    case NoneType.head => Attempt.failure(Err("absence of type is not allowed in values"))
    case ArrayEmptyType.head => Attempt.successful(ArrayEmptyType)
    case head if head >= ArrayUnindexedType.minByte && head <= ArrayUnindexedType.maxByte => Attempt.successful(ArrayUnindexedType(head))
    case head if head >= ArrayIndexedType.minByte && head <= ArrayIndexedType.maxByte => Attempt.successful(ArrayIndexedType(head))
    case ObjectEmptyType.head => Attempt.successful(ObjectEmptyType)
    case head if head >= ObjectSortedType.minByte && head <= ObjectSortedType.maxByte => Attempt.successful(ObjectSortedType(head))
    case head if head >= ObjectUnsortedType.minByte && head <= ObjectUnsortedType.maxByte => Attempt.successful(ObjectUnsortedType(head))
    case ArrayCompactType.head => Attempt.successful(ArrayCompactType)
    case ObjectCompactType.head => Attempt.successful(ObjectCompactType)
    case IllegalType.head => Attempt.successful(IllegalType)
    case NullType.head => Attempt.successful(NullType)
    case FalseType.head => Attempt.successful(FalseType)
    case TrueType.head => Attempt.successful(TrueType)
    case DoubleType.head => Attempt.successful(DoubleType)
    case DateType.head => Attempt.successful(DateType)
    case MinKeyType.head => Attempt.successful(MinKeyType)
    case MaxKeyType.head => Attempt.successful(MaxKeyType)
    case head if head >= 0x20 && head <= 0x27 => Attempt.successful(IntSignedType(head))
    case head if head >= 0x28 && head <= 0x2f => Attempt.successful(IntUnsignedType(head))
    case head if head >= SmallintPositiveType.minByte && head <= SmallintPositiveType.maxByte => Attempt.successful(SmallintPositiveType(head))
    case head if head >= SmallintNegativeType.minByte && head <= SmallintNegativeType.maxByte => Attempt.successful(SmallintNegativeType(head))
    case head if head >= StringShortType.minByte && head <= StringShortType.maxByte => Attempt.successful(StringShortType(head))
    case StringLongType.head => Attempt.successful(StringLongType)
    case head if head >= BinaryType.minByte && head <= BinaryType.maxByte => Attempt.successful(BinaryType(head))
    case u => Attempt.failure(Err(s"unknown head byte ${u.toHexString}"))
  })

  /**
   * encodes the type to the head byte
   */
  val vpackTypeEncoder: Encoder[VPackType] = Encoder(_.bits.pure[Attempt])

  /**
   * type codec
   */
  val vpackTypeCodec: Codec[VPackType] = Codec(vpackTypeEncoder, vpackTypeDecoder)
}
