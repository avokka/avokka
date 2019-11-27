package avokka.velocypack.codecs

import avokka.velocypack.VPackArray
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{provide, uint8L, vlong, vlongL}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Err, SizeBound}
import shapeless.HList

class VPackArrayCodec(compact: Boolean) extends Codec[VPackArray] {

  override def sizeBound: SizeBound = SizeBound.atLeast(8)

  override def encode(value: VPackArray): Attempt[BitVector] = {
    value.values match {
      case Nil => Attempt.successful(BitVector(0x01))

      case values if compact => {
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

      case values @ AllSameSize(size) => {
        val valuesBytes = values.length * size / 8
        val lengthMax = 1 + 8 + valuesBytes
        val (lengthBytes, head) = lengthUtils(lengthMax)
        val arrayBytes = 1 + lengthBytes + valuesBytes
        val len = ulongBytes(arrayBytes, lengthBytes)

        (BitVector(0x02 + head) ++ len ++ values.reduce(_ ++ _)).pure[Attempt]
      }

      case values => {
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

        val result = if (head == 3) BitVector(0x06 + head) ++ len ++ valuesAll ++ index ++ nr
                               else BitVector(0x06 + head) ++ len ++ nr ++ valuesAll ++ index
        result.pure[Attempt]
      }
    }
  }

  private def decoderLinear(lenLength: Int): Decoder[Seq[BitVector]] = Decoder( b =>
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

  private val decoderLinear1 = decoderLinear(1)
  private val decoderLinear2 = decoderLinear(2)
  private val decoderLinear4 = decoderLinear(4)
  private val decoderLinear8 = decoderLinear(8)

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

  private val decoderOffsets1 = decoderOffsets(1)
  private val decoderOffsets2 = decoderOffsets(2)
  private val decoderOffsets4 = decoderOffsets(4)

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

  private val decoderOffsets8 = decoderOffsets64(8)

  private val decoderSingle: Decoder[BitVector] = Decoder( bits =>
    VPackLengthDecoder.decodeValue(bits).map { len =>
      DecodeResult(bits.take(8 * len), bits.drop(8 * len))
    }
  )

  private val decoderCompact: Decoder[Seq[BitVector]] = Decoder( b =>
    for {
      length  <- vlongL.decode(b)
      bodyLen = 8 * (length.value - 1 - vlongLength(length.value))
      body    <- scodec.codecs.bits(bodyLen).decode(length.remainder)
      nr      <- vlongL.decode(body.value.reverseByteOrder)
      result  <- Decoder.decodeCollect(decoderSingle, Some(nr.value.toInt))(body.value)
    } yield DecodeResult(result.value, body.remainder)
  )

  private val emptyProvider: Codec[Seq[BitVector]] = provide(Seq.empty[BitVector])

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackArray]] = {
    for {
      head     <- uint8L.decode(bits).ensure(Err("not a vpack array"))(h => (h.value >= 0x01 && h.value <= 0x09) || h.value == 0x13)
      decs     <- (head.value match {
        case 0x01 => emptyProvider
        case 0x02 => decoderLinear1
        case 0x03 => decoderLinear2
        case 0x04 => decoderLinear4
        case 0x05 => decoderLinear8
        case 0x06 => decoderOffsets1
        case 0x07 => decoderOffsets2
        case 0x08 => decoderOffsets4
        case 0x09 => decoderOffsets8
        case 0x13 => decoderCompact
      }).decode(head.remainder)
    } yield decs.map(VPackArray.apply)
  }

  def list[T](codec: Codec[T]): Codec[List[T]] = exmap(
    _.values.toList.traverse(codec.decodeValue),
    _.traverse(codec.encode).map(VPackArray.apply)
  )

  def vector[T](codec: Codec[T]): Codec[Vector[T]] = exmap(
    _.values.toVector.traverse(codec.decodeValue),
    _.traverse(codec.encode).map(VPackArray.apply)
  )

  def hlist[C <: HList, A <: HList](codecs: C)(implicit ev: VPackHListCodec[C, A]): Codec[A] = VPackHListCodec.codec(codecs)
  def hlistCompact[C <: HList, A <: HList](codecs: C)(implicit ev: VPackHListCodec[C, A]): Codec[A] = VPackHListCodec.codecCompact(codecs)
}

object VPackArrayCodec extends VPackArrayCodec(false) {
  object Compact extends VPackArrayCodec(true)
}
