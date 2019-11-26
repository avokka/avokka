package avokka.velocypack

import java.time.Instant

import cats.implicits._
import codecs._
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
  implicit val codec: Codec[VPackDouble] = { constant(0x1b) ~> doubleL }.as
}

case class VPackDate(value: Long) extends VPackValue

object VPackDate {
  implicit val codec: Codec[VPackDate] = { constant(0x1c) ~> int64L }.as
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

case class VPackLong(value: Long) extends VPackValue

object VPackLong {
  implicit val codec: Codec[VPackLong] = VPackLongCodec
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

object VPackValue {

  implicit val codec: Codec[VPackValue] = lazily { Codec.coproduct[VPackValue].choice }

  val vpBool: Codec[Boolean] = VPackBooleanCodec.as
  val vpString: Codec[String] = VPackStringCodec.as

  val vpDouble: Codec[Double] = VPackDouble.codec.as
  val vpFloat: Codec[Float] = vpDouble.xmap(_.toFloat, _.toDouble)

  val vpInstant: Codec[Instant] = VPackDate.codec.xmap(d => Instant.ofEpochMilli(d.value), t => VPackDate(t.toEpochMilli))

  val vpInt: Codec[Int] = VPackLongCodec.narrow({
    case VPackLong(value) if value.isValidInt => Attempt.successful(value.toInt)
    case VPackLong(value) => Attempt.failure(Err(s"Long to Int failure for value $value"))
  }, v => VPackLong(v.toLong))
  val vpLong: Codec[Long] = VPackLongCodec.as

  val vpBin: Codec[ByteVector] = VPackBinaryCodec.as



}
