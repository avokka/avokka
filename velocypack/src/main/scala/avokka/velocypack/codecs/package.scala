package avokka.velocypack

import cats.implicits._
import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.interop.cats._

import scala.annotation.tailrec

package object codecs {

  /**
   * calculates byte length of unsigned long
   *
   * @param value the long
   * @param acc accumulator
   * @return length in bytes
   */
  @tailrec def ulongLength(value: Long, acc: Int = 1): Int = {
    if (value > 0XffL) ulongLength(value >> 8, acc + 1)
    else acc
  }

  /**
   * calculates byte length of variable length long
   * @param value the long
   * @param acc accumulator
   * @return length in bytes
   */
  @tailrec def vlongLength(value: Long, acc: Long = 1): Long = {
    if (value >= 0X80L) vlongLength(value >> 7, acc + 1)
    else acc
  }

  /**
   * builds a bit vector of a Long in little endian
   * @param value the long
   * @param size length in bytes
   * @return bit vector
   */
  def ulongBytes(value: Long, size: Int): BitVector = BitVector.fromLong(value, size * 8, ByteOrdering.LittleEndian)

  def ulongLA(bits: Int): Codec[Long] = if (bits < 64) ulongL(bits) else longL(bits)

  private[codecs] object AllSameSize {
    def unapply(s: Iterable[BitVector]): Option[Long] = for {
      size <- s.headOption.map(_.size) if s.forall(_.size == size)
    } yield size
  }

  val vpackEncoder: Encoder[VPackValue] = Encoder(_ match {
    case v : VPackArray => VPackArrayCodec.encoder.encode(v)
    case v : VPackObject => VPackObjectCodec.encoderSorted.encode(v)
    case VPackIllegal => VPackType.IllegalType.bits.pure[Attempt]
    case VPackNull => VPackType.NullType.bits.pure[Attempt]
    case VPackValue.False => VPackType.FalseType.bits.pure[Attempt]
    case VPackValue.True => VPackType.TrueType.bits.pure[Attempt]
    case v : VPackDouble => VPackDoubleCodec.encoder.encode(v)
    case v : VPackDate => VPackDateCodec.encoder.encode(v)
    case VPackMinKey => VPackType.MinKeyType.bits.pure[Attempt]
    case VPackMaxKey => VPackType.MaxKeyType.bits.pure[Attempt]
    case v : VPackLong => VPackLongCodec.encoder.encode(v)
    case v : VPackSmallint => VPackSmallintCodec.encoder.encode(v)
    case v : VPackString => VPackStringCodec.encoder.encode(v)
    case v : VPackBinary => VPackBinaryCodec.encoder.encode(v)
  })

  val vpackDecoder: Decoder[VPackValue] = VPackType.decoder.flatMap {
    case t : VPackType.ArrayUnindexedType => VPackArrayCodec.decoderLinear(t)
    case t : VPackType.ArrayIndexedType if t.head == 0x09 => VPackArrayCodec.decoderOffsets64(t)
    case t : VPackType.ArrayIndexedType => VPackArrayCodec.decoderOffsets(t)
    case t : VPackType.ObjectSortedType if t.head == 0x0e => VPackObjectCodec.decoderOffsets64(t)
    case t : VPackType.ObjectSortedType => VPackObjectCodec.decoderOffsets(t)
    case t : VPackType.ObjectUnsortedType if t.head == 0x12 => VPackObjectCodec.decoderOffsets64(t)
    case t : VPackType.ObjectUnsortedType => VPackObjectCodec.decoderOffsets(t)
    case VPackType.ArrayCompactType => VPackArrayCodec.decoderCompact
    case VPackType.ObjectCompactType => VPackObjectCodec.decoderCompact
    case VPackType.DoubleType => VPackDoubleCodec.decoder
    case VPackType.DateType => VPackDateCodec.decoder
    case t : VPackType.IntSignedType => VPackLongCodec.decoderSigned(t)
    case t : VPackType.IntUnsignedType => VPackLongCodec.decoderUnsigned(t)
    case t : VPackType.SmallintPositiveType => VPackSmallintCodec.decoderPositive(t)
    case t : VPackType.SmallintNegativeType => VPackSmallintCodec.decoderNegative(t)
    case t : VPackType.StringShortType => VPackStringCodec.decoder(t)
    case t @ VPackType.StringLongType => VPackStringCodec.decoder(t)
    case t : VPackType.BinaryType => VPackBinaryCodec.decoder(t)
    case t : VPackType.SingleByte => provide(t.singleton)
  }

  val vpackCodec: Codec[VPackValue] = Codec(vpackEncoder, vpackDecoder)
}
