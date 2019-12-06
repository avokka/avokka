package avokka.velocypack.codecs

import avokka.velocypack.{VPackObject, VPackString}
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{provide, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}

import scala.collection.SortedMap

class VPackObjectCodec(compact: Boolean, sorted: Boolean) extends Codec[VPackObject] with VPackCompoundCodec {

  override def sizeBound: SizeBound = SizeBound.atLeast(8)

  val keyValueEncoder: Encoder[(String, BitVector)] = Encoder( kv =>
    for {
      k <- VPackStringCodec.encode(VPackString(kv._1))
    } yield k ++ kv._2
  )

  val emptyByte = 0x0a
  val compactByte = 0x14

  override def encode(value: VPackObject): Attempt[BitVector] = {
    value.values match {
      case values if values.isEmpty => BitVector(emptyByte).pure[Attempt]

      case values if compact => for {
        valuesAll <- values.toList.traverse(keyValueEncoder.encode)
        result <- encodeCompact(compactByte, valuesAll)
      } yield result

      case values => {
        for {
          keyValues <- values.toList.traverse(kv => keyValueEncoder.encode(kv).map(b => kv._1 -> b))
          val (valuesAll, valuesBytes, offsets) = keyValues.foldLeft((BitVector.empty, 0L, Map.empty[String, Long])) {
            case ((bytes, offset, offsets), (key, value)) => {
              (bytes ++ value, offset + value.size / 8, offsets.updated(key, offset))
            }
          }
          val lengthMax = 1 + 8 + 8 + valuesBytes + 8 * offsets.size
          val (lengthBytes, head) = lengthUtils(lengthMax)
          val headBytes = 1 + lengthBytes + lengthBytes
          val indexTable = offsets.mapValues(off => headBytes + off)

          val len = ulongBytes(headBytes + valuesBytes + lengthBytes * offsets.size, lengthBytes)
          val nr = ulongBytes(offsets.size, lengthBytes)
          val sor = if (sorted) SortedMap(indexTable.toSeq: _*) else indexTable
          val index = sor.foldLeft(BitVector.empty)((b, l) => b ++ ulongBytes(l._2, lengthBytes))

          val headBase = if (sorted) 0x0b else 0x0f
          val result = if (head == 3) BitVector(headBase + head) ++ len ++ valuesAll ++ index ++ nr
          else BitVector(headBase + head) ++ len ++ nr ++ valuesAll ++ index

        } yield result
      }
    }
  }

  def decoderOffsets(lenLength: Int): Decoder[Map[String, BitVector]] = Decoder( b =>
    for {
      length  <- ulongLA(8 * lenLength).decode(b)
      nr      <- ulongLA(8 * lenLength).decode(length.remainder)
      bodyOffset = 1 + lenLength + lenLength
      bodyLen = length.value - bodyOffset
      body    <- scodec.codecs.bits(8 * bodyLen).decode(nr.remainder)
      values  <- scodec.codecs.bits(8 * (bodyLen - nr.value * lenLength)).decode(body.value)
      offsets <- Decoder.decodeCollect[Vector, Long](ulongLA(8 * lenLength), Some(nr.value.toInt))(values.remainder)
      result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.value.size / 8).map {
        case (from, until) => values.value.slice(8 * from, 8 * until)
      }
      rr      <- result.toList.traverse(decoderSingle.decodeValue)
    } yield DecodeResult(rr.toMap, body.remainder)
  )

  private val decoderOffsets1 = decoderOffsets(1)
  private val decoderOffsets2 = decoderOffsets(2)
  private val decoderOffsets4 = decoderOffsets(4)

  def decoderOffsets64(lenLength: Int): Decoder[Map[String, BitVector]] = Decoder( b =>
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
      rr        <- result.toList.traverse(decoderSingle.decodeValue)
    } yield DecodeResult(rr.toMap, remainder)
  )

  private val decoderOffsets8 = decoderOffsets64(8)

  private val decoderSingle: Decoder[(String, BitVector)] = Decoder( bits =>
    for {
      key <- VPackStringCodec.decode(bits)
      value = key.remainder
      len <- VPackHeadLengthDecoder.decodeValue(value)
    } yield DecodeResult(key.value.value -> value.take(8 * len.length), value.drop(8 * len.length))
  )

  private val decoderCompact: Decoder[Map[String, BitVector]] = Decoder( bits =>
    for {
      length  <- VPackVLongCodec.decode(bits)
      bodyLen = 8 * (length.value - 1 - vlongLength(length.value))
      body    <- scodec.codecs.bits(bodyLen).decode(length.remainder)
      nr      <- VPackVLongCodec.decode(body.value.reverseByteOrder)
      result  <- Decoder.decodeCollect(decoderSingle, Some(nr.value.toInt))(body.value)
    } yield DecodeResult(result.value.toMap, body.remainder)
  )

  private val emptyProvider: Codec[Map[String, BitVector]] = provide(Map.empty[String, BitVector])

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackObject]] = {
    for {
      head     <- uint8L.decode(bits).ensure(Err("not a vpack object"))(h => (h.value >= emptyByte && h.value <= 0x12) || h.value == compactByte)
      decs     <- (head.value match {
        case `emptyByte` => emptyProvider
        case 0x0b => decoderOffsets1
        case 0x0c => decoderOffsets2
        case 0x0d => decoderOffsets4
        case 0x0e => decoderOffsets8
        case 0x0f => decoderOffsets1
        case 0x10 => decoderOffsets2
        case 0x11 => decoderOffsets4
        case 0x12 => decoderOffsets8
        case `compactByte` => decoderCompact
      }).decode(head.remainder)
    } yield decs.map(va => VPackObject(va))
  }

  def mapOf[T](codec: Codec[T]): Codec[Map[String, T]] = exmap(
    _.values.toList.traverse({ case (k,v) => codec.decodeValue(v).map(r => k -> r) }).map(_.toMap),
    _.toList.traverse({ case (k,v) => codec.encode(v).map(r => k -> r) }).map(l => VPackObject(l.toMap))
  )

}

object VPackObjectCodec extends VPackObjectCodec(false, true) {
  object Compact extends VPackObjectCodec(true, false)
  object Unsorted extends VPackObjectCodec(false, false)
}
