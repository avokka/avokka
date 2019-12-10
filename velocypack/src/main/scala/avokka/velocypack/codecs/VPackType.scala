package avokka.velocypack.codecs

import scodec.codecs._
import scodec.{Attempt, Codec, Decoder, Err}

trait VPackType {
  def head: Int
}

trait VPackTypeLength extends VPackType {
  def lengthSize: Int
  def lengthDecoder: Decoder[Long]
}

object VPackType {
  case object Void extends VPackType {
    override val head: Int = 0x00
  }

  case object ArrayEmpty extends VPackType {
    override val head: Int = 0x01
  }

  case class ArrayUnindexed(override val head: Int) extends VPackTypeLength {
    require(head >= 0x02 && head <= 0x05)
    override val lengthSize: Int = 1 << (head - 0x02)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }

  case class ArrayIndexed(override val head: Int) extends VPackTypeLength {
    require(head >= 0x06 && head <= 0x09)
    override val lengthSize: Int = 1 << (head - 0x06)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }

  case object ObjectEmpty extends VPackType {
    override val head: Int = 0x0a
  }

  case class ObjectSorted(override val head: Int) extends VPackTypeLength {
    require(head >= 0x0b && head <= 0x0e)
    override val lengthSize: Int = 1 << (head - 0x0b)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize).map(_ - 1 - lengthSize)
  }

  case class ObjectUnsorted(override val head: Int) extends VPackTypeLength {
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

  case object Null extends VPackType {
    override val head: Int = 0x18
  }

  case object False extends VPackType {
    override val head: Int = 0x19
  }
  case object True extends VPackType {
    override val head: Int = 0x1a
  }

  case object Double extends VPackType {
    override val head: Int = 0x1b
  }

  case object Date extends VPackType {
    override val head: Int = 0x1c
  }

  case object MinKey extends VPackType {
    override val head: Int = 0x1e
  }
  case object MaxKey extends VPackType {
    override val head: Int = 0x1f
  }

  case class IntSigned(override val head: Int) extends VPackTypeLength {
    require(head >= 0x20 && head <= 0x27)
    override val lengthSize: Int = 0
    override val lengthDecoder: Decoder[Long] = provide(head - 0x20 + 1)
  }
  case class IntUnsigned(override val head: Int) extends VPackTypeLength {
    require(head >= 0x28 && head <= 0x2f)
    override val lengthSize: Int = 0
    override val lengthDecoder: Decoder[Long] = provide(head - 0x28 + 1)
  }

  case class SmallintPositive(override val head: Int) extends VPackType {
    require(head >= 0x30 && head <= 0x39)
  }
  case class SmallintNegative(override val head: Int) extends VPackType {
    require(head >= 0x3a && head <= 0x3f)
  }

  case class StringShort(override val head: Int) extends VPackTypeLength {
    require(head >= 0x40 && head <= 0xbe)
    override val lengthSize: Int = 0
    override val lengthDecoder: Decoder[Long] = provide(head - 0x40)
  }
  case object StringLong extends VPackTypeLength {
    override val head: Int = 0xbf
    override val lengthSize: Int = 8
    override val lengthDecoder: Decoder[Long] =  int64L
  }

  case class Binary(override val head: Int) extends VPackTypeLength {
    require(head >= 0xc0 && head <= 0xc7)
    override val lengthSize: Int = head - 0xc0 + 1
    override val lengthDecoder: Decoder[Long] = ulongLA(8 * lengthSize)
  }

  implicit val codec: Codec[VPackType] = uint8L.exmap({
    case Void.head => Attempt.failure(Err("absence of type is not allowed in values"))
    case ArrayEmpty.head => Attempt.successful(ArrayEmpty)
    case head if head >= 0x02 && head <= 0x05 => Attempt.successful(ArrayUnindexed(head))
    case head if head >= 0x06 && head <= 0x09 => Attempt.successful(ArrayIndexed(head))
    case ObjectEmpty.head => Attempt.successful(ObjectEmpty)
    case head if head >= 0x0b && head <= 0x0e => Attempt.successful(ObjectSorted(head))
    case head if head >= 0x0f && head <= 0x12 => Attempt.successful(ObjectUnsorted(head))
    case ArrayCompact.head => Attempt.successful(ArrayCompact)
    case ObjectCompact.head => Attempt.successful(ObjectCompact)
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
  }
    , h => Attempt.successful(h.head))
}
