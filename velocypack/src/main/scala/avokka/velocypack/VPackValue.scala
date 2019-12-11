package avokka.velocypack

import avokka.velocypack.codecs.{VPackArrayCodec, VPackBinaryCodec, VPackBooleanCodec, VPackDateCodec, VPackDoubleCodec, VPackLongCodec, VPackObjectCodec, VPackSmallintCodec, VPackStringCodec, VPackType, VPackVLongCodec}
import scodec.{Codec, Decoder, Encoder}
import scodec.bits._
import scodec.codecs.provide

sealed trait VPackValue

case object VPackVoid extends VPackValue

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

  object From {
    def unapply(i: Int): Option[VPackSmallint] = fromNumeric(i)
    def unapply(l: Long): Option[VPackSmallint] = fromNumeric(l)
    def unapply(d: Double): Option[VPackSmallint] = if (d.isWhole()) fromNumeric(d) else None
  }
}

case class VPackLong(value: Long) extends VPackValue

object VPackLong {
  object From {
    def unapply(i: Int): Option[VPackLong] = Some(VPackLong(i.toLong))
    def unapply(d: Double): Option[VPackLong] = if (d.isWhole()) Some(VPackLong(d.toLong)) else None
  }
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

  val vpackEncoder: Encoder[VPackValue] = Encoder(_ match {
    case v : VPackArray => VPackArrayCodec.encoder.encode(v)
    case v : VPackObject => VPackObjectCodec.encoderSorted.encode(v)
    case VPackIllegal => VPackType.Illegal.pureBits
    case VPackNull => VPackType.Null.pureBits
    case False => VPackType.False.pureBits
    case True => VPackType.True.pureBits
    case v : VPackDouble => VPackDoubleCodec.encoder.encode(v)
    case v : VPackDate => VPackDateCodec.encoder.encode(v)
    case VPackMinKey => VPackType.MinKey.pureBits
    case VPackMaxKey => VPackType.MaxKey.pureBits
    case v : VPackLong => VPackLongCodec.encoder.encode(v)
    case v : VPackSmallint => VPackSmallintCodec.encoder.encode(v)
    case v : VPackString => VPackStringCodec.encoder.encode(v)
    case v : VPackBinary => VPackBinaryCodec.encoder.encode(v)
  })

  val vpackDecoder: Decoder[VPackValue] = VPackType.typeDecoder.flatMap {
    case t : VPackType.ArrayUnindexed => VPackArrayCodec.decoderLinear(t)
    case t : VPackType.ArrayIndexed if t.head == 0x09 => VPackArrayCodec.decoderOffsets64(t)
    case t : VPackType.ArrayIndexed => VPackArrayCodec.decoderOffsets(t)
    case t : VPackType.ObjectSorted if t.head == 0x0e => VPackObjectCodec.decoderOffsets64(t)
    case t : VPackType.ObjectSorted => VPackObjectCodec.decoderOffsets(t)
    case t : VPackType.ObjectUnsorted if t.head == 0x12 => VPackObjectCodec.decoderOffsets64(t)
    case t : VPackType.ObjectUnsorted => VPackObjectCodec.decoderOffsets(t)
    case VPackType.ArrayCompact => VPackArrayCodec.decoderCompact
    case VPackType.ObjectCompact => VPackObjectCodec.decoderCompact
    case VPackType.Double => VPackDoubleCodec.decoder
    case VPackType.Date => VPackDateCodec.decoder
    case t : VPackType.IntSigned => VPackLongCodec.decoderSigned(t)
    case t : VPackType.IntUnsigned => VPackLongCodec.decoderUnsigned(t)
    case t : VPackType.SmallintPositive => VPackSmallintCodec.decoderPositive(t)
    case t : VPackType.SmallintNegative => VPackSmallintCodec.decoderNegative(t)
    case t : VPackType.StringShort => VPackStringCodec.decoder(t)
    case t @ VPackType.StringLong => VPackStringCodec.decoder(t)
    case t : VPackType.Binary => VPackBinaryCodec.decoder(t)
    case t : VPackType.SingleByte => provide(t.singleton)
    // case VPackType.ArrayUnindexed(b) => arrayCodec.map(VArray.apply)
  }

  val vpackCodec: Codec[VPackValue] = Codec(vpackEncoder, vpackDecoder)
}