package avokka.velocypack

import cats.syntax.applicative._
import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.interop.cats._

import scala.annotation.tailrec

package object codecs {
  import VPackType._

  /**
    * calculates byte length of unsigned long
    *
    * @param value the long
    * @param acc accumulator
    * @return length in bytes
    */
  @tailrec private[codecs] def ulongLength(value: Long, acc: Int = 1): Int = {
    if (value > 0XffL) ulongLength(value >> 8, acc + 1)
    else acc
  }

  /**
    * calculates byte length of variable length long
    * @param value the long
    * @param acc accumulator
    * @return length in bytes
    */
  @tailrec private[codecs] def vlongLength(value: Long, acc: Long = 1): Long = {
    if (value >= 0X80L) vlongLength(value >> 7, acc + 1)
    else acc
  }

  /**
    * builds a bit vector of a Long in little endian
    * @param value the long
    * @param size length in bytes
    * @return bit vector
    */
  private[codecs] def ulongBytes(value: Long, size: Int): BitVector = BitVector.fromLong(value, size * 8, ByteOrdering.LittleEndian)

  private[velocypack] def ulongLA(bits: Int): Codec[Long] = if (bits < 64) ulongL(bits) else longL(bits)

  private[codecs] object AllSameSize {
    def unapply(s: Iterable[BitVector]): Option[Long] =
      for {
        size <- s.headOption.map(_.size) if s.forall(_.size == size)
      } yield size
  }

  val vpackEncoder: Encoder[VPack] = Encoder(_ match {
    case VNone        => BitVector.empty.pure[Attempt]
    case v: VArray    => VPackArrayCodec.encoder.encode(v)
    case v: VObject   => VPackObjectCodec.encoderSorted.encode(v)
    case VIllegal     => IllegalType.bits.pure[Attempt]
    case VNull        => NullType.bits.pure[Attempt]
    case v: VBoolean  => VPackBooleanCodec.encoder.encode(v)
    case v: VDouble   => VPackDoubleCodec.encoder.encode(v)
    case v: VDate     => VPackDateCodec.encoder.encode(v)
    case VMinKey      => MinKeyType.bits.pure[Attempt]
    case VMaxKey      => MaxKeyType.bits.pure[Attempt]
    case v: VLong     => VPackLongCodec.encoder.encode(v)
    case v: VSmallint => VPackSmallintCodec.encoder.encode(v)
    case v: VString   => VPackStringCodec.encoder.encode(v)
    case v: VBinary   => VPackBinaryCodec.encoder.encode(v)
  })

  val vpackDecoder: Decoder[VPack] = VPackTypeCodec.decoder.flatMap {
    case t: ArrayUnindexedType                                         => VPackArrayCodec.decoderLinear(t)
    case t: ArrayIndexedType if t.header == ArrayIndexedType.maxByte   => VPackArrayCodec.decoderOffsets64(t)
    case t: ArrayIndexedType                                           => VPackArrayCodec.decoderOffsets(t)
    case t: ObjectSortedType if t.header == ObjectSortedType.maxByte   => VPackObjectCodec.decoderOffsets64(t)
    case t: ObjectSortedType                                           => VPackObjectCodec.decoderOffsets(t)
    case t: ObjectUnsortedType if t.header == ObjectUnsortedType.maxByte => VPackObjectCodec.decoderOffsets64(t)
    case t: ObjectUnsortedType                                         => VPackObjectCodec.decoderOffsets(t)
    case ArrayCompactType                                              => VPackArrayCodec.decoderCompact
    case ObjectCompactType                                             => VPackObjectCodec.decoderCompact
    case DoubleType                                                    => VPackDoubleCodec.decoder
    case DateType                                                      => VPackDateCodec.decoder
    case t: IntSignedType                                              => VPackLongCodec.decoderSigned(t)
    case t: IntUnsignedType                                            => VPackLongCodec.decoderUnsigned(t)
    case t: SmallintPositiveType                                       => VPackSmallintCodec.decoderPositive(t)
    case t: SmallintNegativeType                                       => VPackSmallintCodec.decoderNegative(t)
    case t: StringShortType                                            => VPackStringCodec.decoder(t)
    case t: StringLongType.type                                        => VPackStringCodec.decoder(t)
    case t: BinaryType                                                 => VPackBinaryCodec.decoder(t)
    case t: SingleByte                                                 => provide(t.singleton)
    case NoneType                                                      => fail(Err("absence of type is not allowed in values"))
  }

  val vpackCodec: Codec[VPack] = Codec(vpackEncoder, vpackDecoder)

}
