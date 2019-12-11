package avokka.velocypack.codecs

import avokka.velocypack.{VPackArray, VPackIllegal, VPackMaxKey, VPackMinKey, VPackNull, VPackValue}
import scodec.bits.BitVector
import scodec.codecs._
import scodec.{Attempt, Codec, Decoder, Encoder, Err}

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
  def pureBits: Attempt[BitVector] = Attempt.successful(BitVector(head))
}

object VPackType {

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
   * @param singleton the value associated
   */
  abstract class SingleByte
  (
    override val head: Int,
    val singleton: VPackValue
  ) extends VPackType

  case object Void extends VPackType { override val head: Int = 0x00 }

  case object ArrayEmpty extends SingleByte(0x01, VPackValue.ArrayEmpty)

  case class ArrayUnindexed(override val head: Int) extends VPackType with WithLength {
    require(head >= 0x02 && head <= 0x05)
    override val lengthSize: Int = 1 << (head - 0x02)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }

  case class ArrayIndexed(override val head: Int) extends VPackType with WithLength {
    require(head >= 0x06 && head <= 0x09)
    override val lengthSize: Int = 1 << (head - 0x06)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }

  case object ObjectEmpty extends SingleByte(0x0a, VPackValue.ObjectEmpty)

  trait ObjectTrait extends VPackType with WithLength

  case class ObjectSorted(override val head: Int) extends ObjectTrait {
    require(head >= 0x0b && head <= 0x0e)
    override val lengthSize: Int = 1 << (head - 0x0b)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }

  case class ObjectUnsorted(override val head: Int) extends ObjectTrait {
    require(head >= 0x0f && head <= 0x12)
    override val lengthSize: Int = 1 << (head - 0x0f)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }

  case object ArrayCompact extends VPackType {
    override val head: Int = 0x13
 //   override val lengthDecoder: Decoder[Long] = VPackVLongCodec.map(l => l - 1 - vlongLength(l))
  }

  case object ObjectCompact extends VPackType {
    override val head: Int = 0x14
    //  override val lengthDecoder: Decoder[Long] = VPackVLongCodec.map(l => l - 1 - vlongLength(l))
  }

  case object Illegal extends SingleByte(0x17, VPackIllegal)
  case object Null extends SingleByte(0x18, VPackNull)

  case object False extends SingleByte(0x19, VPackValue.False)
  case object True extends SingleByte(0x1a, VPackValue.True)

  case object Double extends VPackType {
    override val head: Int = 0x1b
  }

  case object Date extends VPackType {
    override val head: Int = 0x1c
  }

  case object MinKey extends SingleByte(0x1e, VPackMinKey)
  case object MaxKey extends SingleByte(0x1f, VPackMaxKey)

  case class IntSigned(override val head: Int) extends VPackType with WithLength {
    require(head >= 0x20 && head <= 0x27)
    override val lengthSize: Int = head - 0x20 + 1
    override val lengthDecoder: Decoder[Long] = provide(0)
  }
  case class IntUnsigned(override val head: Int) extends VPackType with WithLength {
    require(head >= 0x28 && head <= 0x2f)
    override val lengthSize: Int = head - 0x28 + 1
    override val lengthDecoder: Decoder[Long] = provide(0)
  }

  case class SmallintPositive(override val head: Int) extends VPackType {
    require(head >= 0x30 && head <= 0x39)
  }
  case class SmallintNegative(override val head: Int) extends VPackType {
    require(head >= 0x3a && head <= 0x3f)
  }

  case class StringShort(override val head: Int) extends VPackType with WithLength {
    require(head >= 0x40 && head <= 0xbe)
    override val lengthSize: Int = 0
    override val lengthDecoder: Decoder[Long] = provide(head - 0x40)
  }
  case object StringLong extends VPackType with WithLength {
    override val head: Int = 0xbf
    override val lengthSize: Int = 8
    override val lengthDecoder: Decoder[Long] = int64L
  }

  case class Binary(override val head: Int) extends VPackType with WithLength {
    require(head >= 0xc0 && head <= 0xc7)
    override val lengthSize: Int = head - 0xc0 + 1
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize)
  }

  val typeDecoder: Decoder[VPackType] = uint8L.emap({
    case Void.head => Attempt.failure(Err("absence of type is not allowed in values"))
    case ArrayEmpty.head => Attempt.successful(ArrayEmpty)
    case head if head >= 0x02 && head <= 0x05 => Attempt.successful(ArrayUnindexed(head))
    case head if head >= 0x06 && head <= 0x09 => Attempt.successful(ArrayIndexed(head))
    case ObjectEmpty.head => Attempt.successful(ObjectEmpty)
    case head if head >= 0x0b && head <= 0x0e => Attempt.successful(ObjectSorted(head))
    case head if head >= 0x0f && head <= 0x12 => Attempt.successful(ObjectUnsorted(head))
    case ArrayCompact.head => Attempt.successful(ArrayCompact)
    case ObjectCompact.head => Attempt.successful(ObjectCompact)
    case Illegal.head => Attempt.successful(Illegal)
    case Null.head => Attempt.successful(Null)
    case False.head => Attempt.successful(False)
    case True.head => Attempt.successful(True)
    case Double.head => Attempt.successful(Double)
    case Date.head => Attempt.successful(Date)
    case MinKey.head => Attempt.successful(MinKey)
    case MaxKey.head => Attempt.successful(MaxKey)
    case head if head >= 0x20 && head <= 0x27 => Attempt.successful(IntSigned(head))
    case head if head >= 0x28 && head <= 0x2f => Attempt.successful(IntUnsigned(head))
    case head if head >= 0x30 && head <= 0x39 => Attempt.successful(SmallintPositive(head))
    case head if head >= 0x3a && head <= 0x3f => Attempt.successful(SmallintNegative(head))
    case head if head >= 0x40 && head <= 0xbe => Attempt.successful(StringShort(head))
    case StringLong.head => Attempt.successful(StringLong)
    case head if head >= 0xc0 && head <= 0xc7 => Attempt.successful(Binary(head))
    case u => Attempt.failure(Err(s"unknown head byte ${u.toHexString}"))
  })

  val typeEncoder: Encoder[VPackType] = Encoder(_.pureBits)

  val codec: Codec[VPackType] = Codec(typeEncoder, typeDecoder)
}
