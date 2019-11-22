package avokka.velocypack

import scodec.{Attempt, Codec, DecodeResult, Decoder, Err}
import scodec.bits._
import scodec.codecs._
import shapeless.{::, HList, HNil}

trait VelocypackArrayDecoder[D <: HList, A <: HList] {
  def decodeLinear(decoders: D, values: BitVector): Attempt[A]
  def decodeOffsets(decoders: D, values: BitVector, offsets: Seq[Long]): Attempt[A]
}

object VelocypackArrayDecoder {

  implicit object hnilDecoder extends VelocypackArrayDecoder[HNil, HNil] {
    override def decodeLinear(decoders: HNil, values: BitVector): Attempt[HNil] = Attempt.successful(HNil)
    override def decodeOffsets(decoders: HNil, values: BitVector, offsets: Seq[Long]): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsDecoder[T, Dec, D <: HList, A <: HList](implicit ev: VelocypackArrayDecoder[D, A], eve: Dec <:< Decoder[T]): VelocypackArrayDecoder[Dec :: D, T :: A] = new VelocypackArrayDecoder[Dec :: D, T :: A] {
    override def decodeLinear(decoders: Dec :: D, values: BitVector): Attempt[T :: A] = {
      for {
        rl <- decoders.head.decode(values)
        rr <- ev.decodeLinear(decoders.tail, rl.remainder)
      } yield rl.value :: rr
    }
    override def decodeOffsets(decoders: Dec :: D, values: BitVector, offsets: Seq[Long]): Attempt[T :: A] = {
      val offset = offsets.head
      val value = values.drop(offset * 8)
      for {
        rl <- decoders.head.decode(value).map(_.value)
        rr <- ev.decodeOffsets(decoders.tail, values, offsets.tail)
      } yield rl :: rr
    }
  }

  def ulongLA(bits: Int): Codec[Long] = if (bits < 64) ulongL(bits) else longL(bits)

  def vpArray[D <: HList, A <: HList](decoders: D)(implicit ev: VelocypackArrayDecoder[D, A]): Decoder[A] = new Decoder[A] {

    def decodeLinear(lenLength: Int, b: BitVector): Attempt[DecodeResult[A]] = for {
      length <- ulongLA(lenLength).decode(b)
      bodyLen = 8 * length.value - 8 - lenLength
      body   <- bits(bodyLen).decode(length.remainder) // length.remainder.splitAt(bodyLen)
      values = body.value.bytes.dropWhile(_ == 0).bits
      result <- ev.decodeLinear(decoders, values)
    } yield DecodeResult(result, body.remainder)

    def decodeOffsets(lenLength: Int, b: BitVector): Attempt[DecodeResult[A]] = for {
      length <- ulongLA(lenLength).decode(b)
      nr     <- ulongLA(lenLength).decode(length.remainder)
      bodyOffset = 8 + lenLength + lenLength
      bodyLen = 8 * length.value - bodyOffset
      (body, remainder) = nr.remainder.splitAt(bodyLen)
      (values, index) = body.splitAt(bodyLen - nr.value * lenLength)
      offsets <- Decoder.decodeCollect(ulongLA(lenLength), Some(nr.value.toInt))(index)
      result  <- ev.decodeOffsets(decoders, values, offsets.value.map(_ - bodyOffset / 8))
    } yield DecodeResult(result, remainder)

    def decodeOffsets64(lenLength: Int, b: BitVector): Attempt[DecodeResult[A]] = for {
      length <- ulongLA(lenLength).decode(b)
      bodyOffset = 8 + lenLength
      bodyLen = 8 * length.value - bodyOffset
      (body, remainder) = length.remainder.splitAt(bodyLen)
      (valuesIndex, number) = body.splitAt(bodyLen - lenLength)
      nr     <- ulongLA(lenLength).decode(number)
      (values, index) = valuesIndex.splitAt(bodyLen - nr.value * lenLength - lenLength)
      offsets <- Decoder.decodeCollect(ulongLA(lenLength), Some(nr.value.toInt))(index)
      result  <- ev.decodeOffsets(decoders, values, offsets.value.map(_ - bodyOffset / 8))
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
          case 0x01 if ev == hnilDecoder => ev.decodeLinear(decoders, head.remainder).map(n => DecodeResult(n, head.remainder))
          case 0x01 => Attempt.failure(Err("empty array for non empty decoders"))
          case 0x02 => decodeLinear(8, head.remainder)
          case 0x03 => decodeLinear(16, head.remainder)
          case 0x04 => decodeLinear(32, head.remainder)
          case 0x05 => decodeLinear(64, head.remainder)
          case 0x06 => decodeOffsets(8, head.remainder)
          case 0x07 => decodeOffsets(16, head.remainder)
          case 0x08 => decodeOffsets(32, head.remainder)
          case 0x09 => decodeOffsets64(64, head.remainder)
          case 0x13 => decodeCompact(head.remainder)
          case _ => Attempt.failure(Err("not a vpack array"))
        }
      } yield res
    }
  }

  def main(args: Array[String]): Unit = {

    val dec = vpArray(VPackValue.vpInt :: VPackValue.vpBool :: HNil)
    println(dec.decode(hex"02 05 00 31 19".bits))

    val ex = vpArray(VPackValue.vpInt :: VPackValue.vpInt :: VPackValue.vpInt :: HNil)
    println(ex.decode(hex"01".bits))
    println(ex.decode(hex"02 05 31 32 33".bits))
    println(ex.decode(hex"03 06 00 31 32 33".bits))
    println(ex.decode(hex"04 08 00 00 00 31 32 33".bits))
    println(ex.decode(hex"05 0c 00 00 00 00 00 00 00 31 32 33".bits))
    println(ex.decode(hex"06 09 03 31 32 33 03 04 05".bits))
    println(ex.decode(hex"07 0e 00 03 00 31 32 33 05 00 06 00 07 00".bits))
    println(ex.decode(hex"08 18 00 00 00 03 00 00 00 31 32 33 09 00 00 00 0a 00 00 00 0b 00 00 00".bits))
    println(ex.decode(hex"09 2c 00 00 00 00 00 00 00 31 32 33 09 00 00 00 00 00 00 00 0a 00 00 00 00 00 00 00 0b 00 00 00 00 00 00 00 03 00 00 00 00 00 00 00".bits))

    val c = vpArray(VPackValue.vpInt :: VPackValue.vpInt :: HNil)
    println(c.decode(hex"13 06 31 28 10 02".bits))
  }
}