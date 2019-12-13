package avokka.velocypack.codecs

import avokka.velocypack.VPack.VArray
import cats.data.Chain
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.traverse._
import cats.syntax.foldable._
import cats.instances.vector._
import scodec.bits.BitVector
import scodec.interop.cats._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}

import scala.language.higherKinds

/**
 * Codec of velocypack arrays
 */
object VPackArrayCodec extends VPackCompoundCodec {
  import VPackType.{ArrayUnindexedType, ArrayIndexedType, ArrayCompactType, ArrayEmptyType}

  /**
   * compact encoder
   */
  val encoderCompact: Encoder[VArray] = Encoder(_.values match {
    // empty array
    case values if values.isEmpty => ArrayEmptyType.bits.pure[Attempt]
    // encode elements
    case values => for {
      valuesBits <- values.traverse(vpackEncoder.encode)
      result <- encodeCompact(ArrayCompactType.head, valuesBits)
    } yield result
  })

  /**
   * standard encoder
   */
  val encoder: Encoder[VArray] = Encoder(_.values match {
    // empty array
    case values if values.isEmpty => ArrayEmptyType.bits.pure[Attempt]
    // encode elements
    case values => values.traverse(vpackEncoder.encode).map({
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
        val (valuesAll, valuesBytes, offsets) = values.foldLeft((BitVector.empty, 0L, Chain.empty[Long])) {
          case ((bytes, offset, offsets), element) => (bytes ++ element, offset + element.size / 8, offsets :+ offset)
        }
        val lengthMax = 1 + 8 + 8 + valuesBytes + 8 * offsets.length
        val (lengthBytes, head) = lengthUtils(lengthMax)
        val headBytes = 1 + lengthBytes + lengthBytes
        val indexTable = offsets.map(_ + headBytes)

        val len = ulongBytes(headBytes + valuesBytes + lengthBytes * offsets.length, lengthBytes)
        val nr = ulongBytes(offsets.length, lengthBytes)
        val index = indexTable.foldLeft(BitVector.empty)((b, l) => b ++ ulongBytes(l, lengthBytes))

        if (head == 3) BitVector(0x06 + head) ++ len ++ valuesAll ++ index ++ nr
                  else BitVector(0x06 + head) ++ len ++ nr ++ valuesAll ++ index
      }
    })
  })

  def decoderLinear(t: ArrayUnindexedType): Decoder[VArray] = Decoder(b =>
    for {
      length  <- t.lengthDecoder.decode(b)
      body    <- scodec.codecs.bits(8 * length.value).decode(length.remainder)
      values  = body.value.bytes.dropWhile(_ == 0)
      result  <- Decoder.decodeCollect(vpackDecoder, None)(values.bits)
      //valueLen <- VPackHeadLengthDecoder.decodeValue(values.bits)
      //nr = (values.size / valueLen.length).toInt
      //result = Seq.range(0, nr).map(n => values.slice(n * valueLen.length, (n + 1) * valueLen.length).bits)
    } yield DecodeResult(VArray(Chain.fromSeq(result.value)), body.remainder)
  )

  def decoderOffsets(t: ArrayIndexedType): Decoder[VArray] = Decoder(b =>
    for {
      length  <- t.lengthDecoder.decode(b)
      nr      <- ulongLA(8 * t.lengthSize).decode(length.remainder)
      bodyOffset = 1 + t.lengthSize + t.lengthSize
      bodyLen = length.value - t.lengthSize
      body    <- scodec.codecs.bits(8 * bodyLen).decode(nr.remainder)
      values  <- scodec.codecs.bits(8 * (bodyLen - nr.value * t.lengthSize)).decode(body.value)
      offsets <- Decoder.decodeCollect(ulongLA(8 * t.lengthSize), Some(nr.value.toInt))(values.remainder)
      result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.value.size / 8).map {
        case (from, until) => values.value.slice(8 * from, 8 * until)
      }
      a       <- result.toVector.traverse(vpackDecoder.decodeValue)
    } yield DecodeResult(VArray(Chain.fromSeq(a)), body.remainder)
  )

  def decoderOffsets64(t: ArrayIndexedType): Decoder[VArray] = Decoder(b =>
    for {
      length    <- t.lengthDecoder.decode(b)
      bodyOffset = 1 + t.lengthSize
      bodyLen    = length.value
      (body, remainder) = length.remainder.splitAt(8 * bodyLen)
      (valuesIndex, number) = body.splitAt(8 * (bodyLen - t.lengthSize))
      nr        <- ulongLA(8 * t.lengthSize).decode(number)
      (values, index) = valuesIndex.splitAt(8 * (bodyLen - nr.value * t.lengthSize - t.lengthSize))
      offsets   <- Decoder.decodeCollect(ulongLA(8 * t.lengthSize), Some(nr.value.toInt))(index)
      result = offsetsToRanges(offsets.value.map(_ - bodyOffset), values.size / 8).map {
        case (from, until) => values.slice(8 * from, 8 * until)
      }
      a       <- result.toVector.traverse(vpackDecoder.decodeValue)
    } yield DecodeResult(VArray(Chain.fromSeq(a)), remainder)
  )

  val decoderCompact: Decoder[VArray] = Decoder(b =>
    for {
      length  <- VPackVLongCodec.decode(b)
      bodyLen = 8 * (length.value - 1 - vlongLength(length.value))
      body    <- scodec.codecs.bits(bodyLen).decode(length.remainder)
      nr      <- VPackVLongCodec.decode(body.value.reverseByteOrder)
      result  <- Decoder.decodeCollect(vpackDecoder, Some(nr.value.toInt))(body.value)
    } yield DecodeResult(VArray(Chain.fromSeq(result.value)), body.remainder)
  )

  /*
  private val emptyProvider: Codec[Seq[BitVector]] = provide(Seq.empty[BitVector])

  override def decode(bits: BitVector): Attempt[DecodeResult[VPackArray]] = {
    for {
      head     <- uint8L.decode(bits).ensure(Err("not a vpack array"))(h => (h.value >= emptyByte && h.value <= 0x09) || h.value == compactByte)
      decs     <- (head.value match {
        case `emptyByte` => emptyProvider
        case 0x02 => decoderLinear1
        case 0x03 => decoderLinear2
        case 0x04 => decoderLinear4
        case 0x05 => decoderLinear8
        case 0x06 => decoderOffsets1
        case 0x07 => decoderOffsets2
        case 0x08 => decoderOffsets4
        case 0x09 => decoderOffsets8
        case `compactByte` => decoderCompact
      }).decode(head.remainder)
    } yield decs.map(VPackArray.apply)
  }
*/
  /*
  def list[T](codec: Codec[T]): Codec[List[T]] = exmap(
    _.values.toList.traverse(codec.decodeValue),
    _.traverse(codec.encode).map(VPackArray.apply)
  )
*/
  /*
  def traverse[T, A[_]](codec: Codec[T])(implicit tr: Traverse[A], ap: Applicative[A], mk: MonoidK[A]): Codec[A[T]] = exmap(
    _.values
      .foldLeft(mk.empty[BitVector])((acc, t) => mk.combineK(acc, ap.pure(t)))
      .traverse(codec.decodeValue)
    ,
    _.traverse(codec.encode).map(bs => VPackArray(bs.toList))
  )

  def vector[T](codec: Codec[T]): Codec[Vector[T]] = traverse[T, Vector](codec)
  def list[T](codec: Codec[T]): Codec[List[T]] = traverse[T, List](codec)
*/
  val codec: Codec[VArray] = Codec(encoder, vpackDecoder.emap({
    case v: VArray => v.pure[Attempt]
    case _ => Err("not a vpack array").raiseError
  }))

  val codecCompact: Codec[VArray] = Codec(encoderCompact, vpackDecoder.emap({
    case v: VArray => v.pure[Attempt]
    case _ => Err("not a vpack array").raiseError
  }))
}

/*
object VPackArrayCodec extends VPackArrayCodec(false) {
  object Compact extends VPackArrayCodec(true)
}
*/