package avokka.velocypack

import java.time.Instant

import cats.implicits._
import codecs.{VPackDoubleCodec, _}
import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.interop.cats._

sealed trait VPackValue

case object VPackReserved1 extends VPackValue {
  implicit val codec: Codec[VPackReserved1.type] = constant(0x15) ~> provide(VPackReserved1)
}

case object VPackReserved2 extends VPackValue {
  implicit val codec: Codec[VPackReserved2.type] = constant(0x16) ~> provide(VPackReserved2)
}

case object VPackIllegal extends VPackValue {
  implicit val codec: Codec[VPackIllegal.type] = constant(0x17) ~> provide(VPackIllegal)
}

case object VPackNull extends VPackValue {
  implicit val codec: Codec[VPackNull.type] = constant(0x18) ~> provide(VPackNull)
}

case class VPackBoolean(value: Boolean) extends VPackValue

object VPackBoolean {
  implicit val codec: Codec[VPackBoolean] = VPackBooleanCodec
}

case class VPackDouble(value: Double) extends VPackValue

object VPackDouble {
  implicit val codec: Codec[VPackDouble] = VPackDoubleCodec
}

case class VPackDate(value: Long) extends VPackValue

object VPackDate {
  implicit val codec: Codec[VPackDate] = VPackDateCodec
}

case object VPackExternal extends VPackValue {
  implicit val codec: Codec[VPackExternal.type] = constant(0x1d) ~> provide(VPackExternal)
}

case object VPackMinKey extends VPackValue {
  implicit val codec: Codec[VPackMinKey.type] = constant(0x1e) ~> provide(VPackMinKey)
}

case object VPackMaxKey extends VPackValue {
  implicit val codec: Codec[VPackMaxKey.type] = constant(0x1f) ~> provide(VPackMaxKey)
}

case class VPackSmallint(value: Byte) extends VPackValue

object VPackSmallint {
  def unapply(i: Int): Option[VPackSmallint] = test(i)
  def unapply(l: Long): Option[VPackSmallint] = test(l)

  implicit val codec: Codec[VPackSmallint] = VPackSmallintCodec

  def test[T](arg: T)(implicit num: Numeric[T]): Option[VPackSmallint] = {
    VPackSmallintCodec.can(arg).map(VPackSmallint.apply)
  }

  def unapply(arg: Double): Option[VPackSmallint] = {
    if (arg.isWhole()) test(arg) else None
  }
}

case class VPackLong(value: Long) extends VPackValue

object VPackLong {
  implicit val codec: Codec[VPackLong] = VPackLongCodec

  def unapply(arg: Int): Option[VPackLong] = Some(VPackLong(arg.toLong))

  def unapply(arg: Double): Option[VPackLong] = {
    if (arg.isWhole()) Some(VPackLong(arg.toLong)) else None
  }
}

case class VPackString(value: String) extends VPackValue

object VPackString {
  implicit val codec: Codec[VPackString] = VPackStringCodec
}

case class VPackBinary(value: ByteVector) extends VPackValue

object VPackBinary {
  implicit val codec: Codec[VPackBinary] = VPackBinaryCodec
}

case class VPackArray(values: Seq[BitVector]) extends VPackValue

object VPackArray {
  implicit val codec: Codec[VPackArray] = VPackArrayCodec
  val codecCompact: Codec[VPackArray] = VPackArrayCodec.Compact
}

case class VPackObject(values: Map[String, BitVector]) extends VPackValue

object VPackObject {
  implicit val codec: Codec[VPackObject] = VPackObjectCodec
  val codecCompact: Codec[VPackObject] = VPackObjectCodec.Compact
  val codecUnsorted: Codec[VPackObject] = VPackObjectCodec.Unsorted
}

object VPackValue {

  implicit val codec: Codec[VPackValue] = lazily { Codec.coproduct[VPackValue].choice }

  val vpBool: Codec[Boolean] = VPackBooleanCodec.as
  val vpString: Codec[String] = VPackStringCodec.as

  val vpInt: Codec[Int] = new Codec[Int] {
    override def sizeBound: SizeBound = VPackSmallintCodec.sizeBound | VPackLongCodec.sizeBound

    override def encode(value: Int): Attempt[BitVector] = value match {
      case VPackSmallint(s) => VPackSmallintCodec.encode(s)
      case VPackLong(l) => VPackLongCodec.encode(l)
    }

    override def decode(bits: BitVector): Attempt[DecodeResult[Int]] = Decoder.choiceDecoder(
      VPackSmallintCodec.map(_.value.toInt),
      VPackLongCodec.map(_.value).emap({
        case l if l.isValidInt => l.toInt.pure[Attempt]
        case _ => Err("vpack long overflow").raiseError
      })
    ).decode(bits)
  }

  val vpLong: Codec[Long] = new Codec[Long] {
    override def sizeBound: SizeBound = VPackSmallintCodec.sizeBound | VPackLongCodec.sizeBound

    override def encode(value: Long): Attempt[BitVector] = value match {
      case VPackSmallint(s) => VPackSmallintCodec.encode(s)
      case l => VPackLongCodec.encode(VPackLong(l))
    }

    override def decode(bits: BitVector): Attempt[DecodeResult[Long]] = Decoder.choiceDecoder(
      VPackSmallintCodec.map(_.value.toLong),
      VPackLongCodec.map(_.value)
    ).decode(bits)
  }

  val vpDouble: Codec[Double] = new Codec[Double] {
    override def sizeBound: SizeBound = VPackSmallintCodec.sizeBound | VPackDoubleCodec.sizeBound | VPackLongCodec.sizeBound

    override def encode(value: Double): Attempt[BitVector] = value match {
      case VPackSmallint(s) => VPackSmallintCodec.encode(s)
      case VPackLong(l) => VPackLongCodec.encode(l)
      case d => VPackDoubleCodec.encode(VPackDouble(d))
    }

    override def decode(bits: BitVector): Attempt[DecodeResult[Double]] = Decoder.choiceDecoder(
      VPackSmallintCodec.map(_.value.toDouble),
      VPackLongCodec.map(_.value.toDouble),
      VPackDoubleCodec.map(_.value),
    ).decode(bits)
  }

  val vpFloat: Codec[Float] = vpDouble.xmap(_.toFloat, _.toDouble)

  val vpInstant: Codec[Instant] = VPackDate.codec.xmap(d => Instant.ofEpochMilli(d.value), t => VPackDate(t.toEpochMilli))

  val vpBin: Codec[ByteVector] = VPackBinaryCodec.as
}
