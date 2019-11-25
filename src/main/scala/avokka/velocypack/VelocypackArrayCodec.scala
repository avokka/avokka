package avokka.velocypack

import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}
import scodec.bits._
import scodec.codecs._
import shapeless.{::, HList, HNil}

trait VelocypackArrayCodec[C <: HList, A <: HList] {
  def encode(encoders: C, arguments: A): Attempt[Seq[BitVector]]
  def decodeLinear(decoders: C, values: BitVector): Attempt[A]
  def decodeOffsets(decoders: C, values: BitVector, offsets: Seq[Long]): Attempt[A]
}

object VelocypackArrayCodec {

//  def apply[E <: HList, A <: HList](implicit encoders: VelocypackArrayEncoder[E, A]): VelocypackArrayEncoder[E, A] = encoders

  implicit object hnilCodec extends VelocypackArrayCodec[HNil, HNil] {
    override def encode(encoders: HNil, arguments: HNil): Attempt[Seq[BitVector]] = Attempt.successful(Vector.empty)
    override def decodeLinear(decoders: HNil, values: BitVector): Attempt[HNil] = Attempt.successful(HNil)
    override def decodeOffsets(decoders: HNil, values: BitVector, offsets: Seq[Long]): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsCodec[T, Cod, C <: HList, A <: HList](implicit ev: VelocypackArrayCodec[C, A], eve: Cod <:< Codec[T]): VelocypackArrayCodec[Cod :: C, T :: A] = new VelocypackArrayCodec[Cod :: C, T :: A] {
    override def encode(encoders: Cod :: C, arguments: T :: A): Attempt[Seq[BitVector]] = {
      for {
        rl <- encoders.head.encode(arguments.head)
        rr <- ev.encode(encoders.tail, arguments.tail)
      } yield rl +: rr
    }
    override def decodeLinear(decoders: Cod :: C, values: BitVector): Attempt[T :: A] = {
      for {
        rl <- decoders.head.decode(values)
        rr <- ev.decodeLinear(decoders.tail, rl.remainder)
      } yield rl.value :: rr
    }
    override def decodeOffsets(decoders: Cod :: C, values: BitVector, offsets: Seq[Long]): Attempt[T :: A] = {
      val offset = offsets.head
      val value = values.drop(offset * 8)
      for {
        rl <- decoders.head.decode(value).map(_.value)
        rr <- ev.decodeOffsets(decoders.tail, values, offsets.tail)
      } yield rl :: rr
    }
  }

  def encoder[E <: HList, A <: HList](encoders: E)(implicit ev: VelocypackArrayCodec[E, A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(encoders, value)
      arr <- VPackArray.encoder.encode(VPackArray(values))
    } yield arr
  }

  def encoderCompact[E <: HList, A <: HList](encoders: E)(implicit ev: VelocypackArrayCodec[E, A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(encoders, value)
      arr <- VPackArray.compactEncoder.encode(VPackArray(values))
    } yield arr
  }

  def decoder[D <: HList, A <: HList](decoders: D)(implicit ev: VelocypackArrayCodec[D, A]): Decoder[A] = new Decoder[A] {

    def decodeLinear(lenLength: Int, b: BitVector): Attempt[DecodeResult[A]] = for {
      length  <- codecs.ulongLA(8 * lenLength).decode(b)
      bodyLen = length.value - 1 - lenLength
      body    <- bits(8 * bodyLen).decode(length.remainder)
      values  = body.value.bytes.dropWhile(_ == 0).bits
      result  <- ev.decodeLinear(decoders, values)
    } yield DecodeResult(result, body.remainder)

    def decodeOffsets(lenLength: Int, b: BitVector): Attempt[DecodeResult[A]] = for {
      length  <- codecs.ulongLA(8 * lenLength).decode(b)
      nr      <- codecs.ulongLA(8 * lenLength).decode(length.remainder)
      bodyOffset = 1 + lenLength + lenLength
      bodyLen = length.value - bodyOffset
      body    <- bits(8 * bodyLen).decode(nr.remainder)
      values  <- bits(8 * (bodyLen - nr.value * lenLength)).decode(body.value)
      offsets <- Decoder.decodeCollect(codecs.ulongLA(8 * lenLength), Some(nr.value.toInt))(values.remainder)
      result  <- ev.decodeOffsets(decoders, values.value, offsets.value.map(_ - bodyOffset))
    } yield DecodeResult(result, body.remainder)

    def decodeOffsets64(lenLength: Int, b: BitVector): Attempt[DecodeResult[A]] = for {
      length    <- codecs.ulongLA(8 * lenLength).decode(b)
      bodyOffset = 1 + lenLength
      bodyLen    = length.value - bodyOffset
      (body, remainder) = length.remainder.splitAt(8 * bodyLen)
      (valuesIndex, number) = body.splitAt(8 * (bodyLen - lenLength))
      nr        <- codecs.ulongLA(8 * lenLength).decode(number)
      (values, index) = valuesIndex.splitAt(8 * (bodyLen - nr.value * lenLength - lenLength))
      offsets   <- Decoder.decodeCollect(codecs.ulongLA(8 * lenLength), Some(nr.value.toInt))(index)
      result    <- ev.decodeOffsets(decoders, values, offsets.value.map(_ - bodyOffset))
    } yield DecodeResult(result, remainder)

    def decodeCompact(b: BitVector): Attempt[DecodeResult[A]] = for {
      length  <- vlongL.decode(b)
      bodyLen = 8 * (length.value - 1 - codecs.vlongLength(length.value))
      body    <- bits(bodyLen).decode(length.remainder)
      result  <- ev.decodeLinear(decoders, body.value)
    } yield DecodeResult(result, body.remainder)

    override def decode(bits: BitVector): Attempt[DecodeResult[A]] = {
      for {
        head <- uint8L.decode(bits)
        res  <- head.value match {
          case 0x01 if ev == hnilCodec => ev.decodeLinear(decoders, head.remainder).map(n => DecodeResult(n, head.remainder))
          case 0x01 => Attempt.failure(Err("empty array for non empty decoders"))
          case 0x02 => decodeLinear(1, head.remainder)
          case 0x03 => decodeLinear(2, head.remainder)
          case 0x04 => decodeLinear(4, head.remainder)
          case 0x05 => decodeLinear(8, head.remainder)
          case 0x06 => decodeOffsets(1, head.remainder)
          case 0x07 => decodeOffsets(2, head.remainder)
          case 0x08 => decodeOffsets(4, head.remainder)
          case 0x09 => decodeOffsets64(8, head.remainder)
          case 0x13 => decodeCompact(head.remainder)
          case _ => Attempt.failure(Err("not a vpack array"))
        }
      } yield res
    }
  }

  def codec[C <: HList, A <: HList](codecs: C)(implicit ev: VelocypackArrayCodec[C, A]): Codec[A] = Codec(encoder(codecs), decoder(codecs))
  def codecCompact[C <: HList, A <: HList](codecs: C)(implicit ev: VelocypackArrayCodec[C, A]): Codec[A] = Codec(encoderCompact(codecs), decoder(codecs))

}
