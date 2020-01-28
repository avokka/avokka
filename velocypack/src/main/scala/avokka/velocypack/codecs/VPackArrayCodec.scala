package avokka.velocypack
package codecs

import cats.data.Chain
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.traverse._
import cats.syntax.foldable._
import cats.instances.vector._
import scodec.bits.BitVector
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import VPackType.{ArrayUnindexedType, ArrayIndexedType, ArrayCompactType, ArrayEmptyType}
import VPack.VArray

/**
  * Codec of velocypack arrays
  */
private object VPackArrayCodec extends VPackCompoundCodec {

  /**
    * compact encoder
    */
  private[codecs] val encoderCompact: Encoder[VArray] = Encoder(_.values match {
    // empty array
    case values if values.isEmpty => ArrayEmptyType.bits.pure[Attempt]
    // encode elements
    case values =>
      for {
        valuesBits <- values.traverse(vpackEncoder.encode)
        result <- encodeCompact(ArrayCompactType.head, valuesBits)
      } yield result
  })

  /**
    * standard encoder
    */
  private[codecs] val encoder: Encoder[VArray] = Encoder(_.values match {
    // empty array
    case values if values.isEmpty => ArrayEmptyType.bits.pure[Attempt]
    // encode elements
    case values =>
      values
        .traverse(vpackEncoder.encode)
        .map({
          // all elements have the same length, no need for index table
          case values @ AllSameSize(size) => {
            val valuesBytes = values.length * size / 8
            val lengthMax = 1 + 8 + valuesBytes
            val (lengthBytes, head) = lengthUtils(lengthMax)
            val arrayBytes = 1 + lengthBytes + valuesBytes
            val len = ulongBytes(arrayBytes, lengthBytes)

            BitVector(0x02 + head) ++ len ++ values.fold // reduce(_ ++ _)
          }

          // build index table offsets
          case values => {
            val (valuesAll, valuesBytes, offsets) =
              values.foldLeft((BitVector.empty, 0L, Chain.empty[Long])) {
                case ((bytes, offset, offsets), element) =>
                  (bytes ++ element, offset + element.size / 8, offsets :+ offset)
              }
            val lengthMax = 1 + 8 + 8 + valuesBytes + 8 * offsets.length
            val (lengthBytes, head) = lengthUtils(lengthMax)
            val headBytes = 1 + lengthBytes + lengthBytes
            val indexTable = offsets.map(_ + headBytes)

            val len =
              ulongBytes(headBytes + valuesBytes + lengthBytes * offsets.length, lengthBytes)
            val nr = ulongBytes(offsets.length, lengthBytes)
            val index =
              indexTable.foldLeft(BitVector.empty)((b, l) => b ++ ulongBytes(l, lengthBytes))

            if (head == 3) BitVector(0x06 + head) ++ len ++ valuesAll ++ index ++ nr
            else BitVector(0x06 + head) ++ len ++ nr ++ valuesAll ++ index
          }
        })
  })

  private[codecs] def decoderLinear(t: ArrayUnindexedType): Decoder[VArray] =
    Decoder(b =>
      for {
        length <- t.lengthDecoder.decode(b)
        body <- scodec.codecs.bits(8 * length.value).decode(length.remainder)
        values = body.value.bytes.dropWhile(_ == 0)
        result <- Decoder.decodeCollect(vpackDecoder, None)(values.bits)
        //valueLen <- VPackHeadLengthDecoder.decodeValue(values.bits)
        //nr = (values.size / valueLen.length).toInt
        //result = Seq.range(0, nr).map(n => values.slice(n * valueLen.length, (n + 1) * valueLen.length).bits)
      } yield DecodeResult(VArray(Chain.fromSeq(result.value)), body.remainder))

  private[codecs] def decoderOffsets(t: ArrayIndexedType): Decoder[VArray] =
    Decoder(b =>
      for {
        length <- t.lengthDecoder.decode(b)
        nr <- ulongLA(8 * t.lengthSize).decode(length.remainder)
        bodyOffset = 1 + t.lengthSize + t.lengthSize
        bodyLen = length.value - t.lengthSize
        body <- scodec.codecs.bits(8 * bodyLen).decode(nr.remainder)
        values <- scodec.codecs.bits(8 * (bodyLen - nr.value * t.lengthSize)).decode(body.value)
        offsets <- Decoder.decodeCollect(ulongLA(8 * t.lengthSize), Some(nr.value.toInt))(
          values.remainder)
        result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.value.size / 8).map {
          case (from, until) => values.value.slice(8 * from, 8 * until)
        }
        a <- result.toVector.traverse(vpackDecoder.decodeValue)
      } yield DecodeResult(VArray(Chain.fromSeq(a)), body.remainder))

  private[codecs] def decoderOffsets64(t: ArrayIndexedType): Decoder[VArray] =
    Decoder(b =>
      for {
        length <- t.lengthDecoder.decode(b)
        bodyOffset = 1 + t.lengthSize
        bodyLen = length.value
        (body, remainder) = length.remainder.splitAt(8 * bodyLen)
        (valuesIndex, number) = body.splitAt(8 * (bodyLen - t.lengthSize))
        nr <- ulongLA(8 * t.lengthSize).decode(number)
        (values, index) = valuesIndex.splitAt(
          8 * (bodyLen - nr.value * t.lengthSize - t.lengthSize))
        offsets <- Decoder.decodeCollect(ulongLA(8 * t.lengthSize), Some(nr.value.toInt))(index)
        result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.size / 8).map {
          case (from, until) => values.slice(8 * from, 8 * until)
        }
        a <- result.toVector.traverse(vpackDecoder.decodeValue)
      } yield DecodeResult(VArray(Chain.fromSeq(a)), remainder))

  private[codecs] val decoderCompact: Decoder[VArray] = Decoder(b =>
    for {
      length <- VPackVLongCodec.decode(b)
      bodyLen = 8 * (length.value - 1 - vlongLength(length.value))
      body <- scodec.codecs.bits(bodyLen).decode(length.remainder)
      nr <- VPackVLongCodec.decode(body.value.reverseByteOrder)
      result <- Decoder.decodeCollect(vpackDecoder, Some(nr.value.toInt))(body.value)
    } yield DecodeResult(VArray(Chain.fromSeq(result.value)), body.remainder))

  private[codecs] val codec: Codec[VArray] = Codec(encoder, vpackDecoder.emap({
    case v: VArray => v.pure[Attempt]
    case _         => Err("not a vpack array").raiseError
  }))

  private[codecs] val codecCompact: Codec[VArray] = Codec(encoderCompact, vpackDecoder.emap({
    case v: VArray => v.pure[Attempt]
    case _         => Err("not a vpack array").raiseError
  }))
}
