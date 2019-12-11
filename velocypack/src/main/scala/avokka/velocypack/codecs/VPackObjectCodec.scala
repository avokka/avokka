package avokka.velocypack.codecs

import avokka.velocypack.{VPackObject, VPackString, VPackValue}
import cats.implicits._
import scodec.bits.BitVector
import scodec.codecs.{TupleCodec, provide, uint8L}
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}
import shapeless.HNil

import scala.collection.SortedMap

object VPackObjectCodec extends VPackCompoundCodec {

  val emptyByte = 0x0a
  val compactByte = 0x14

  private val kvCodec = avokka.velocypack.stringCodec ~~ VPackValue.vpackCodec

  /*
  private val keyValueEncoder: Encoder[(String, VPackValue)] = Encoder(kv =>
    for {
      k <- VPackStringCodec.encoder.encode(VPackString(kv._1))
      v <- VPackValue.vpackEncoder.encode(kv._2)
    } yield k ++ v
  )

  private val keyValueDecoder: Decoder[(String, VPackValue)] = for {
    key   <- avokka.velocypack.stringCodec
    value <- VPackValue.vpackDecoder
  } yield key -> value
*/

  val encoderCompact: Encoder[VPackObject] = new Encoder[VPackObject] {
    override def sizeBound: SizeBound = SizeBound.atLeast(8)

    override def encode(value: VPackObject): Attempt[BitVector] = {
      value.values match {
        case values if values.isEmpty => VPackType.ObjectEmpty.pureBits

        case values => for {
          valuesAll <- values.toList.traverse(kvCodec.encode)
          result <- encodeCompact(compactByte, valuesAll)
        } yield result
      }
    }
  }

  val encoderSorted = encoder(true)
  val encoderUnsorted = encoder(false)

  def encoder(sorted: Boolean): Encoder[VPackObject] = new Encoder[VPackObject] {
    override def sizeBound: SizeBound = SizeBound.atLeast(8)

    override def encode(value: VPackObject): Attempt[BitVector] = {
      value.values match {
        case values if values.isEmpty => VPackType.ObjectEmpty.pureBits

        case values => {
          for {
            keyValues <- values.toList.traverse(kv => kvCodec.encode(kv).map(b => kv._1 -> b))
            (valuesAll, valuesBytes, offsets) = keyValues.foldLeft((BitVector.empty, 0L, Map.empty[String, Long])) {
              case ((bytes, offset, offsets), (key, value)) => {
                (bytes ++ value, offset + value.size / 8, offsets.updated(key, offset))
              }
            }
            lengthMax = 1 + 8 + 8 + valuesBytes + 8 * offsets.size
            (lengthBytes, head) = lengthUtils(lengthMax)
            headBytes = 1 + lengthBytes + lengthBytes
            indexTable = offsets.mapValues(off => headBytes + off)

            len = ulongBytes(headBytes + valuesBytes + lengthBytes * offsets.size, lengthBytes)
            nr = ulongBytes(offsets.size, lengthBytes)
            sor = if (sorted) SortedMap(indexTable.toSeq: _*) else indexTable
            index = sor.foldLeft(BitVector.empty)((b, l) => b ++ ulongBytes(l._2, lengthBytes))

            headBase = if (sorted) 0x0b else 0x0f
            result = if (head == 3) BitVector(headBase + head) ++ len ++ valuesAll ++ index ++ nr
                     else BitVector(headBase + head) ++ len ++ nr ++ valuesAll ++ index

          } yield result
        }
      }
    }
  }

  def decoderOffsets(t: VPackType.ObjectTrait): Decoder[VPackObject] = Decoder( b =>
    for {
      length  <- t.lengthDecoder.decode(b)
      nr      <- ulongLA(8 * t.lengthSize).decode(length.remainder)
      bodyOffset = 1 + t.lengthSize + t.lengthSize
      bodyLen = length.value - bodyOffset
      body    <- scodec.codecs.bits(8 * bodyLen).decode(nr.remainder)
      values  <- scodec.codecs.bits(8 * (bodyLen - nr.value * t.lengthSize)).decode(body.value)
      offsets <- Decoder.decodeCollect[Vector, Long](ulongLA(8 * t.lengthSize), Some(nr.value.toInt))(values.remainder)
      result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.value.size / 8).map {
        case (from, until) => values.value.slice(8 * from, 8 * until)
      }
      rr      <- result.toList.traverse(kvCodec.decodeValue)
    } yield DecodeResult(VPackObject(rr.toMap), body.remainder)
  )

  def decoderOffsets64(t: VPackType.ObjectTrait): Decoder[VPackObject] = Decoder( b =>
    for {
      length    <- t.lengthDecoder.decode(b)
      bodyOffset = 1 + t.lengthSize
      bodyLen    = length.value - bodyOffset
      (body, remainder) = length.remainder.splitAt(8 * bodyLen)
      (valuesIndex, number) = body.splitAt(8 * (bodyLen - t.lengthSize))
      nr        <- ulongLA(8 * t.lengthSize).decode(number)
      (values, index) = valuesIndex.splitAt(8 * (bodyLen - nr.value * t.lengthSize - t.lengthSize))
      offsets   <- Decoder.decodeCollect(ulongLA(8 * t.lengthSize), Some(nr.value.toInt))(index)
      result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.size / 8).map {
        case (from, until) => values.slice(8 * from, 8 * until)
      }
      rr        <- result.toList.traverse(kvCodec.decodeValue)
    } yield DecodeResult(VPackObject(rr.toMap), remainder)
  )

  val decoderCompact: Decoder[VPackObject] = Decoder( bits =>
    for {
      length  <- VPackVLongCodec.decode(bits)
      bodyLen = 8 * (length.value - 1 - vlongLength(length.value))
      body    <- scodec.codecs.bits(bodyLen).decode(length.remainder)
      nr      <- VPackVLongCodec.decode(body.value.reverseByteOrder)
      result  <- Decoder.decodeCollect(kvCodec, Some(nr.value.toInt))(body.value)
    } yield DecodeResult(VPackObject(result.value.toMap), body.remainder)
  )

  /*
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
    _.values.toList.traverse({ case (k,v) => codec.decodeValue(v).map(r => k -> r).mapErr(_.pushContext(k)) }).map(_.toMap),
    _.toList.traverse({ case (k,v) => codec.encode(v).map(r => k -> r).mapErr(_.pushContext(k)) }).map(l => VPackObject(l.toMap))
  )
*/

  val codecSorted: Codec[VPackObject] = Codec(encoderSorted, VPackValue.vpackDecoder.emap({
    case v: VPackObject => v.pure[Attempt]
    case _ => Err("not a vpack object").raiseError
  }))
  val codecUnsorted: Codec[VPackObject] = Codec(encoderUnsorted, VPackValue.vpackDecoder.emap({
    case v: VPackObject => v.pure[Attempt]
    case _ => Err("not a vpack object").raiseError
  }))

  val codecCompact: Codec[VPackObject] = Codec(encoderCompact, VPackValue.vpackDecoder.emap({
    case v: VPackObject => v.pure[Attempt]
    case _ => Err("not a vpack object").raiseError
  }))
}
