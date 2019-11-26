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

  private val emptyArrayResult = BitVector(0x01)

  object AllSameSize {
    def unapply(s: Iterable[BitVector]): Option[Long] = for {
      size <- s.headOption.map(_.size) if s.forall(_.size == size)
    } yield size
  }

  val encoder: Encoder[VPackArray] = Encoder(_ match {

    case VPackArray(Nil) => Attempt.successful(emptyArrayResult)

    case VPackArray(values @ AllSameSize(size)) => {
      val valuesBytes = values.length * size / 8
      val lengthMax = 1 + 8 + valuesBytes
      val (lengthBytes, head) = lengthUtils(lengthMax)
      val arrayBytes = 1 + lengthBytes + valuesBytes
      val len = ulongBytes(arrayBytes, lengthBytes)

      Attempt.successful(BitVector(0x02 + head) ++ len ++ values.reduce(_ ++ _))
    }

    case VPackArray(values) => {
      val (valuesAll, valuesBytes, offsets) = values.foldLeft((BitVector.empty, 0L, Vector.empty[Long])) {
        case ((bytes, offset, offsets), element) => (bytes ++ element, offset + element.size / 8, offsets :+ offset)
      }
      val lengthMax = 1 + 8 + 8 + valuesBytes + 8 * offsets.length
      val (lengthBytes, head) = lengthUtils(lengthMax)
      val headBytes = 1 + lengthBytes + lengthBytes
      val indexTable = offsets.map(off => headBytes + off)

      val len = ulongBytes(headBytes + valuesBytes + lengthBytes * offsets.length, lengthBytes)
      val nr = ulongBytes(offsets.length, lengthBytes)
      val index = indexTable.foldLeft(BitVector.empty)((b, l) => b ++ ulongBytes(l, lengthBytes))

      Attempt.successful(
      if (head == 3) BitVector(0x06 + head) ++ len ++ valuesAll ++ index ++ nr
                else BitVector(0x06 + head) ++ len ++ nr ++ valuesAll ++ index
      )
    }
  })

  val compactEncoder: Encoder[VPackArray] = Encoder(_ match {
    case VPackArray(Nil) => Attempt.successful(emptyArrayResult)
    case VPackArray(values) => {
      val valuesAll = values.reduce(_ ++ _)
      val valuesBytes = valuesAll.size / 8
      for {
        nr <- vlong.encode(values.length)
        lengthBase = 1 + valuesBytes + nr.size / 8
        lengthBaseL = vlongLength(lengthBase)
        lengthT = lengthBase + lengthBaseL
        lenL = vlongLength(lengthT)
        len <- vlong.encode(if (lenL == lengthBaseL) lengthT else lengthT + 1)
      } yield BitVector(0x13) ++ len ++ valuesAll ++ nr.reverseByteOrder
    }
  })

  def decoderLinear(lenLength: Int): Decoder[Seq[BitVector]] = Decoder( b =>
    for {
      length  <- ulongLA(8 * lenLength).decode(b)
      bodyLen = length.value - 1 - lenLength
      body    <- scodec.codecs.bits(8 * bodyLen).decode(length.remainder)
      values  = body.value.bytes.dropWhile(_ == 0)
      valueLen <- VPackLengthDecoder.decode(values.bits)
      nr = (values.size / valueLen.value).toInt
      result = Seq.range(0, nr).map(n => values.slice(n * valueLen.value, (n + 1) * valueLen.value).bits)
    } yield DecodeResult(result, body.remainder)
  )

  def offsetsToRanges(offests: Seq[Long], size: Long): Seq[(Long, Long)] = {
    offests.zipWithIndex.sortBy(_._1).foldRight((Vector.empty[(Int, Long, Long)], size))({
      case ((offset, index), (acc, size)) => (acc :+ (index, offset, size), offset)
    })._1.sortBy(_._1).map(r => r._2 -> r._3)
  }

  def decoderOffsets(lenLength: Int): Decoder[Seq[BitVector]] = Decoder( b =>
    for {
      length  <- ulongLA(8 * lenLength).decode(b)
      nr      <- ulongLA(8 * lenLength).decode(length.remainder)
      bodyOffset = 1 + lenLength + lenLength
      bodyLen = length.value - bodyOffset
      body    <- scodec.codecs.bits(8 * bodyLen).decode(nr.remainder)
      values  <- scodec.codecs.bits(8 * (bodyLen - nr.value * lenLength)).decode(body.value)
      offsets <- Decoder.decodeCollect(ulongLA(8 * lenLength), Some(nr.value.toInt))(values.remainder)
      result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.value.size / 8).map {
        case (from, until) => values.value.slice(8 * from, 8 * until)
      }
    } yield DecodeResult(result, body.remainder)
  )

  def decoderOffsets64(lenLength: Int): Decoder[Seq[BitVector]] = Decoder( b =>
    for {
      length    <- ulongLA(8 * lenLength).decode(b)
      bodyOffset = 1 + lenLength
      bodyLen    = length.value - bodyOffset
      (body, remainder) = length.remainder.splitAt(8 * bodyLen)
      (valuesIndex, number) = body.splitAt(8 * (bodyLen - lenLength))
      nr        <- ulongLA(8 * lenLength).decode(number)
      (values, index) = valuesIndex.splitAt(8 * (bodyLen - nr.value * lenLength - lenLength))
      offsets   <- Decoder.decodeCollect(ulongLA(8 * lenLength), Some(nr.value.toInt))(index)
      result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.size / 8).map {
        case (from, until) => values.slice(8 * from, 8 * until)
      }
    } yield DecodeResult(result, remainder)
  )

  val decoderSingle: Decoder[BitVector] = Decoder( bits =>
    VPackLengthDecoder.decodeValue(bits).map { len =>
      DecodeResult(bits.take(8 * len), bits.drop(8 * len))
    }
  )

  val decoderCompact: Decoder[Seq[BitVector]] = Decoder( b =>
    for {
      length  <- vlongL.decode(b)
      bodyLen = 8 * (length.value - 1 - vlongLength(length.value))
      body    <- scodec.codecs.bits(bodyLen).decode(length.remainder)
      nr      <- vlongL.decode(body.value.reverseByteOrder)
      result  <- Decoder.decodeCollect(decoderSingle, Some(nr.value.toInt))(body.value)
    } yield DecodeResult(result.value, body.remainder)
  )

  val decoder: Decoder[VPackArray] = {
    for {
      head     <- uint8L
      decs     <- head match {
        case 0x01 => provide(Seq.empty[BitVector])
        case 0x02 => decoderLinear(1)
        case 0x03 => decoderLinear(2)
        case 0x04 => decoderLinear(4)
        case 0x05 => decoderLinear(8)
        case 0x06 => decoderOffsets(1)
        case 0x07 => decoderOffsets(2)
        case 0x08 => decoderOffsets(4)
        case 0x09 => decoderOffsets64(8)
        case 0x13 => decoderCompact
        case _ => fail(Err("not a vpack array"))
      }
    } yield VPackArray(decs)
  }

  implicit val codec: Codec[VPackArray] = Codec(encoder, decoder)

  val codecCompact: Codec[VPackArray] = Codec(compactEncoder, decoder)

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

  def vpList[T](codec: Codec[T]): Codec[List[T]] = VPackArray.codec.exmap(
    _.values.toList.traverse(codec.decodeValue),
    _.traverse(codec.encode).map(VPackArray.apply)
  )
  def vpVector[T](codec: Codec[T]): Codec[Vector[T]] = VPackArray.codec.exmap(
    _.values.toVector.traverse(codec.decodeValue),
    _.traverse(codec.encode).map(VPackArray.apply)
  )

}
