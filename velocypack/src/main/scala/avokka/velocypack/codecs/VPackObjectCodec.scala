package avokka.velocypack
package codecs

import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.traverse._
import scodec.bits.BitVector
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import VPack.{VObject, VSmallint, VString}
import VPackType.{ObjectEmptyType, ObjectType, ObjectCompactType}

import scala.collection.SortedMap
import scala.collection.compat._

private[codecs] object VPackObjectCodec extends VPackCompoundCodec {

  private val keyCodec: Codec[String] = Codec(
    VPackStringCodec.encoder.contramap(VString.apply),
    vpackDecoder.emap({
      case VString(s)   => s.pure[Attempt]
      case VSmallint(1) => "_key".pure[Attempt]
      case VSmallint(2) => "_rev".pure[Attempt]
      case VSmallint(3) => "_id".pure[Attempt]
      case VSmallint(4) => "_from".pure[Attempt]
      case VSmallint(5) => "_to".pure[Attempt]
      case v            => Err(s"not a key: $v").raiseError[Attempt, String]
    })
  )

  private val keyValueCodec = keyCodec >>~ { key => vpackCodec.withContext(key) }

  private val encoderCompact: Encoder[VObject] = Encoder(_.values match {
    case values if values.isEmpty => ObjectEmptyType.bits.pure[Attempt]
    case values =>
      for {
        valuesAll <- values.toVector.traverse(keyValueCodec.encode)
        result <- encodeCompact(ObjectCompactType.header, valuesAll)
      } yield result
  })

  private def encoder(sorted: Boolean): Encoder[VObject] =
    Encoder(_.values match {
      case values if values.isEmpty => ObjectEmptyType.bits.pure[Attempt]
      case values => {
        for {
          keyValues <- values.toVector.traverse(kv => keyValueCodec.encode(kv).map(b => kv._1 -> b))
          (valuesAll, valuesBytes, offsets) = keyValues.foldLeft((BitVector.empty, 0L, Map.empty[String, Long])) {
            case ((bytes, offset, offsets), (key, value)) => {
              (bytes ++ value, offset + value.size / 8, offsets.updated(key, offset))
            }
          }
          lengthMax = 1 + 8 + 8 + valuesBytes + 8 * offsets.size
          (lengthBytes, head) = lengthUtils(lengthMax)
          headBytes = 1 + lengthBytes + lengthBytes
          indexTable = offsets.view.mapValues(_ + headBytes)

          len = ulongBytes(headBytes + valuesBytes + lengthBytes * offsets.size, lengthBytes)
          nr = ulongBytes(offsets.size.toLong, lengthBytes)
          sor = if (sorted) SortedMap(indexTable.toSeq: _*) else indexTable
          index = sor.foldLeft(BitVector.empty)((b, l) => b ++ ulongBytes(l._2, lengthBytes))

          headBase = if (sorted) 0x0b else 0x0f
          result = if (head == 3) BitVector(headBase + head) ++ len ++ valuesAll ++ index ++ nr
          else BitVector(headBase + head) ++ len ++ nr ++ valuesAll ++ index

        } yield result
      }
    })

  private[codecs] val encoderSorted: Encoder[VObject] = encoder(true)
  private[codecs] val encoderUnsorted: Encoder[VObject] = encoder(false)

  private[codecs] val decoderCompact: Decoder[VObject] = Decoder(bits =>
    for {
      length <- VPackVLongCodec.decode(bits)
      bodyLen = 8 * (length.value - 1 - vlongLength(length.value))
      body <- scodec.codecs.bits(bodyLen).decode(length.remainder)
      nr <- VPackVLongCodec.decode(body.value.reverseByteOrder)
      result <- Decoder.decodeCollect[Vector, (String, VPack)](keyValueCodec, Some(nr.value.toInt))(body.value)
    } yield DecodeResult(VObject(result.value.toMap), body.remainder))

  private[codecs] def decoderOffsets(t: ObjectType): Decoder[VObject] =
    Decoder(b =>
      for {
        length <- t.lengthDecoder.decode(b)
        nr <- ulongLA(8 * t.lengthSize).decode(length.remainder)
        bodyOffset = 1 + t.lengthSize + t.lengthSize
        bodyLen = length.value - t.lengthSize
        body <- scodec.codecs.bits(8 * bodyLen).decode(nr.remainder)
        values <- scodec.codecs.bits(8 * (bodyLen - nr.value * t.lengthSize)).decode(body.value)
        offsets <- Decoder.decodeCollect[Vector, Long](ulongLA(8 * t.lengthSize), Some(nr.value.toInt))(
          values.remainder)
        result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.value.size / 8).map {
          case (from, until) => values.value.slice(8 * from, 8 * until)
        }
        rr <- result.traverse(keyValueCodec.decodeValue)
      } yield DecodeResult(VObject(rr.toMap), body.remainder))

  private[codecs] def decoderOffsets64(t: ObjectType): Decoder[VObject] =
    Decoder(b =>
      for {
        length <- t.lengthDecoder.decode(b)
        bodyOffset = 1 + t.lengthSize
        bodyLen = length.value
        (body, remainder) = length.remainder.splitAt(8 * bodyLen)
        (valuesIndex, number) = body.splitAt(8 * (bodyLen - t.lengthSize))
        nr <- ulongLA(8 * t.lengthSize).decode(number)
        (values, index) = valuesIndex.splitAt(8 * (bodyLen - nr.value * t.lengthSize - t.lengthSize))
        offsets <- Decoder.decodeCollect[Vector, Long](ulongLA(8 * t.lengthSize), Some(nr.value.toInt))(index)
        result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.size / 8).map {
          case (from, until) => values.slice(8 * from, 8 * until)
        }
        rr <- result.traverse(keyValueCodec.decodeValue)
      } yield DecodeResult(VObject(rr.toMap), remainder))

  private[codecs] val decoder: Decoder[VObject] = vpackDecoder.emap({
    case v: VObject => v.pure[Attempt]
    case _          => Err("not a vpack object").raiseError[Attempt, VObject]
  })

  private[codecs] val codecSorted: Codec[VObject] = Codec(encoderSorted, decoder)
  private[codecs] val codecUnsorted: Codec[VObject] = Codec(encoderUnsorted, decoder)
  private[codecs] val codecCompact: Codec[VObject] = Codec(encoderCompact, decoder)
}
