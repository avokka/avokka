package avokka.velocypack
package codecs

import scodec.{Attempt, Codec, Decoder, Encoder, Err}
import scodec.codecs.uint8L

/**
  * Codec for the first byte of a vpack value
  */
private[codecs] object VPackTypeCodec {
  import VPackType._

  /**
    * decode the head byte to the velocypack type
    */
  private[codecs] val decoder: Decoder[VPackType] = uint8L.emap({
    case NoneType.`header` => Attempt.failure(Err("absence of type is not allowed in values"))

    case ArrayEmptyType.`header` => Attempt.successful(ArrayEmptyType)
    case header if header >= ArrayUnindexedType.minByte && header <= ArrayUnindexedType.maxByte =>
      Attempt.successful(ArrayUnindexedType(header))
    case header if header >= ArrayIndexedType.minByte && header <= ArrayIndexedType.maxByte =>
      Attempt.successful(ArrayIndexedType(header))

    case ObjectEmptyType.`header` => Attempt.successful(ObjectEmptyType)
    case header if header >= ObjectSortedType.minByte && header <= ObjectSortedType.maxByte =>
      Attempt.successful(ObjectSortedType(header))
    case header if header >= ObjectUnsortedType.minByte && header <= ObjectUnsortedType.maxByte =>
      Attempt.successful(ObjectUnsortedType(header))

    case ArrayCompactType.`header`  => Attempt.successful(ArrayCompactType)
    case ObjectCompactType.`header` => Attempt.successful(ObjectCompactType)

    case IllegalType.`header` => Attempt.successful(IllegalType)
    case NullType.`header`    => Attempt.successful(NullType)
    case FalseType.`header`   => Attempt.successful(FalseType)
    case TrueType.`header`    => Attempt.successful(TrueType)
    case DoubleType.`header`  => Attempt.successful(DoubleType)
    case DateType.`header`    => Attempt.successful(DateType)

    case MinKeyType.`header` => Attempt.successful(MinKeyType)
    case MaxKeyType.`header` => Attempt.successful(MaxKeyType)

    case header if header >= IntSignedType.minByte && header <= IntSignedType.maxByte =>
      Attempt.successful(IntSignedType(header))
    case header if header >= IntUnsignedType.minByte && header <= IntUnsignedType.maxByte =>
      Attempt.successful(IntUnsignedType(header))
    case header if header >= SmallintPositiveType.minByte && header <= SmallintPositiveType.maxByte =>
      Attempt.successful(SmallintPositiveType(header))
    case header if header >= SmallintNegativeType.minByte && header <= SmallintNegativeType.maxByte =>
      Attempt.successful(SmallintNegativeType(header))

    case header if header >= StringShortType.minByte && header <= StringShortType.maxByte =>
      Attempt.successful(StringShortType(header))
    case StringLongType.`header` => Attempt.successful(StringLongType)

    case header if header >= BinaryType.minByte && header <= BinaryType.maxByte =>
      Attempt.successful(BinaryType(header))

    case u => Attempt.failure(Err(s"unknown header byte ${u.toHexString}"))
  })

  /**
    * encodes the type to the head byte
    */
  private[codecs] val encoder: Encoder[VPackType] = Encoder(t => Attempt.successful(t.bits))

  /**
    * type codec
    */
  private[codecs] val codec: Codec[VPackType] = Codec(encoder, decoder)
}
