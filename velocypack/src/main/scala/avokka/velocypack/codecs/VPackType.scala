package avokka.velocypack.codecs

import scodec.codecs._
import scodec.{Attempt, Codec, Decoder, Err}

sealed abstract class VPackType(val head: Int) {
  def lengthDecoder: Decoder[Long]
}

object VPackType {
  case object Void extends VPackType(0x00) {
    override val lengthDecoder: Decoder[Long] = provide(0)
  }

  case object ArrayEmpty extends VPackType(0x01) {
    override val lengthDecoder: Decoder[Long] = provide(1)
  }

  case class ArrayUnindexed(override val head: Int) extends VPackType(head) {
    require(head >= 0x02 && head <= 0x05)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 << (head - 0x02))
  }

  case class ArrayIndexed(override val head: Int) extends VPackType(head) {
    require(head >= 0x06 && head <= 0x09)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 << (head - 0x06))
  }

  case object ObjectEmpty extends VPackType(0x0a) {
    override val lengthDecoder: Decoder[Long] = provide(1)
  }

  case class ObjectSorted(override val head: Int) extends VPackType(head) {
    require(head >= 0x0b && head <= 0x0e)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 << (head - 0x0b))
  }

  case class ObjectUnsorted(override val head: Int) extends VPackType(head) {
    require(head >= 0x0f && head <= 0x12)
    override val lengthDecoder: Decoder[Long] = ulongLA(8 << (head - 0x0f))
  }

  case object ArrayCompact extends VPackType(0x13) {
    override val lengthDecoder: Decoder[Long] = VPackVLongCodec
  }

  case object ObjectCompact extends VPackType(0x14) {
    override val lengthDecoder: Decoder[Long] = VPackVLongCodec
  }

  case object Null extends VPackType(0x18) {
    override val lengthDecoder: Decoder[Long] = provide(1)
  }

  case object False extends VPackType(0x19) {
    override val lengthDecoder: Decoder[Long] = provide(1)
  }
  case object True extends VPackType(0x1a) {
    override val lengthDecoder: Decoder[Long] = provide(1)
  }

  case object Double extends VPackType(0x1b) {
    override val lengthDecoder: Decoder[Long] = provide(1 + 8)
  }

  case object Date extends VPackType(0x1c) {
    override val lengthDecoder: Decoder[Long] = provide(1 + 8)
  }

  case object MinKey extends VPackType(0x1e) {
    override val lengthDecoder: Decoder[Long] = provide(1)
  }
  case object MaxKey extends VPackType(0x1f) {
    override val lengthDecoder: Decoder[Long] = provide(1)
  }

  case class IntSigned(override val head: Int) extends VPackType(head) {
    require(head >= 0x20 && head <= 0x27)
    override val lengthDecoder: Decoder[Long] = provide(1 + head - 0x20 + 1)
  }
  case class IntUnsigned(override val head: Int) extends VPackType(head) {
    require(head >= 0x28 && head <= 0x2f)
    override val lengthDecoder: Decoder[Long] = provide(1 + head - 0x28 + 1)
  }

  case class SmallintPositive(override val head: Int) extends VPackType(head) {
    require(head >= 0x30 && head <= 0x39)
    override val lengthDecoder: Decoder[Long] = provide(1)
  }
  case class SmallintNegative(override val head: Int) extends VPackType(head) {
    require(head >= 0x3a && head <= 0x3f)
    override val lengthDecoder: Decoder[Long] = provide(1)
  }

  case class StringShort(override val head: Int) extends VPackType(head) {
    require(head >= 0x40 && head <= 0xbe)
    override val lengthDecoder: Decoder[Long] = provide(1 + head - 0x40)
  }
  case object StringLong extends VPackType(0xbf) {
    override val lengthDecoder: Decoder[Long] = longL(64).map(1L + 8 + _)
  }

  case class Binary(override val head: Int) extends VPackType(head) {
    require(head >= 0xc0 && head <= 0xc7)
    override val lengthDecoder: Decoder[Long] = {
      val l = head - 0xc0 + 1
      ulongLA(8 * l).map(1L + l + _)
    }
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
