package avokka.velocypack

import avokka.velocypack.codecs.{VPackBinaryCodec, VPackBooleanCodec, VPackDateCodec, VPackDoubleCodec, VPackSmallintCodec, VPackStringCodec, VPackType}
import scodec.{Codec, Decoder, Encoder}
import scodec.bits._
import scodec.codecs.provide

sealed trait VPackValue

case object VPackIllegal extends VPackValue

case object VPackNull extends VPackValue

case class VPackBoolean(value: Boolean) extends VPackValue

case class VPackDouble(value: Double) extends VPackValue

case class VPackDate(value: Long) extends VPackValue

case object VPackMinKey extends VPackValue
case object VPackMaxKey extends VPackValue

case class VPackSmallint(value: Byte) extends VPackValue {
  require(-7 < value && value < 10)
}

object VPackSmallint {

  def fromNumeric[T](arg: T)(implicit num: Numeric[T]): Option[VPackSmallint] = {
    if (num.lt(arg, num.fromInt(10)) && num.gt(arg, num.fromInt(-7)))
      Some(VPackSmallint(num.toInt(arg).toByte))
    else None
  }

  def unapply(i: Int): Option[VPackSmallint] = fromNumeric(i)
  def unapply(l: Long): Option[VPackSmallint] = fromNumeric(l)
  def unapply(d: Double): Option[VPackSmallint] = if (d.isWhole()) fromNumeric(d) else None
}

case class VPackLong(value: Long) extends VPackValue

object VPackLong {
  def unapply(i: Int): Option[VPackLong] = Some(VPackLong(i.toLong))
  def unapply(d: Double): Option[VPackLong] = if (d.isWhole()) Some(VPackLong(d.toLong)) else None
}

case class VPackString(value: String) extends VPackValue

case class VPackBinary(value: ByteVector) extends VPackValue

case class VPackArray(values: Seq[VPackValue] = List.empty) extends VPackValue

case class VPackObject(values: Map[String, VPackValue] = Map.empty) extends VPackValue

object VPackValue {
  val False: VPackValue = VPackBoolean(false)
  val True: VPackValue = VPackBoolean(true)
  val ArrayEmpty: VPackValue = VPackArray(Vector.empty)
  val ObjectEmpty: VPackValue = VPackObject(Map.empty)

  val vpackDecoder: Decoder[VPackValue] = VPackType.codec.flatMap {
    case VPackType.Null => provide(VPackNull)
    case VPackType.False => provide(False)
    case VPackType.True => provide(True)
    case VPackType.ArrayEmpty => provide(ArrayEmpty)
    case t : VPackType.StringShort => VPackStringCodec.decoder(t)
    case t @ VPackType.StringLong => VPackStringCodec.decoder(t)
    case t : VPackType.Binary => VPackBinaryCodec.decoder(t)
    case VPackType.Double => VPackDoubleCodec.decoder
    case VPackType.Date => VPackDateCodec.decoder
    case VPackType.MinKey => provide(VPackMinKey)
    case VPackType.MaxKey => provide(VPackMaxKey)
    case t : VPackType.SmallintPositive => VPackSmallintCodec.decoderPositive(t)
    case t : VPackType.SmallintNegative => VPackSmallintCodec.decoderNegative(t)
    // case VPackType.ArrayUnindexed(b) => arrayCodec.map(VArray.apply)
  }

  //implicit val vpackDecoder: Decoder[VPack] = vpackDiscriminated.asDecoder

  val vpackEncoder: Encoder[VPackValue] = Encoder(_ match {
    case VPackNull => VPackType.codec.encode(VPackType.Null)
    case v : VPackBoolean => VPackBooleanCodec.encoder.encode(v)
    // case VPackArray(value) => arrayCodec.encode(value)
    case v : VPackString => VPackStringCodec.encoder.encode(v)
    case v : VPackBinary => VPackBinaryCodec.encoder.encode(v)
    case v : VPackDouble => VPackDoubleCodec.encoder.encode(v)
    case v : VPackDate => VPackDateCodec.encoder.encode(v)
    case VPackMinKey => VPackType.codec.encode(VPackType.MinKey)
    case VPackMaxKey => VPackType.codec.encode(VPackType.MaxKey)
    case v : VPackSmallint => VPackSmallintCodec.encoder.encode(v)
  })

  val vpackCodec: Codec[VPackValue] = Codec(vpackEncoder, vpackDecoder)
}