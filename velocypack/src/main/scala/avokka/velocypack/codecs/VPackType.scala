package avokka.velocypack.codecs

import scodec.codecs.uint8L
import scodec.{Attempt, Codec, Err}

sealed abstract class VPackType(val head: Int) {
}

object VPackType {
  case object Void extends VPackType(0x00)

  case object ArrayEmpty extends VPackType(0x01)

  case class ArrayUnindexed(override val head: Int) extends VPackType(head) {
    require(head >= 0x02 && head <= 0x05)
  }

  case class ArrayIndexed(override val head: Int) extends VPackType(head) {
    require(head >= 0x06 && head <= 0x09)
  }

  case object ObjectEmpty extends VPackType(0x0a)

  case class ObjectSorted(override val head: Int) extends VPackType(head) {
    require(head >= 0x0b && head <= 0x0e)
  }

  case class ObjectUnsorted(override val head: Int) extends VPackType(head) {
    require(head >= 0x0f && head <= 0x12)
  }

  case object ArrayCompact extends VPackType(0x13)

  case object ObjectCompact extends VPackType(0x14)

  case object Null extends VPackType(0x18)

  case object False extends VPackType(0x19)
  case object True extends VPackType(0x1a)

  case object Double extends VPackType(0x1b)

  case object Date extends VPackType(0x1c)

  case object MinKey extends VPackType(0x1e)
  case object MaxKey extends VPackType(0x1f)

  case class IntSigned(override val head: Int) extends VPackType(head) {
    require(head >= 0x20 && head <= 0x27)
  }
  case class IntUnsigned(override val head: Int) extends VPackType(head) {
    require(head >= 0x28 && head <= 0x2f)
  }

  case class SmallintPositive(override val head: Int) extends VPackType(head) {
    require(head >= 0x30 && head <= 0x39)
  }
  case class SmallintNegative(override val head: Int) extends VPackType(head) {
    require(head >= 0x3a && head <= 0x3f)
  }

  case class StringShort(override val head: Int) extends VPackType(head) {
    require(head >= 0x40 && head <= 0xbe)
  }
  case object StringLong extends VPackType(0xbf)

  case class Binary(override val head: Int) extends VPackType(head) {
    require(head >= 0xc0 && head <= 0xc7)
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
