package avokka.velocypack

import scodec._
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

  def vpArray[D <: HList, A <: HList](decoders: D)(implicit ev: VelocypackArrayDecoder[D, A]): Decoder[A] = new Decoder[A] {

    def decodeLinear(length: DecodeResult[Long], lenLength: Long): Attempt[DecodeResult[A]] = {
      val bodyLen = 8 * length.value - 8 - lenLength
      val (body, r) = length.remainder.splitAt(bodyLen)
      val values = body.bytes.dropWhile(_ == 0)
      ev.decodeLinear(decoders, values.bits).map(res => DecodeResult(res, r))
    }

    override def decode(bits: BitVector): Attempt[DecodeResult[A]] = {
      for {
        head <- uint8L.decode(bits)
        res  <- head.value match {
          case 0x02 => ulongL(8).decode(head.remainder).flatMap(len => decodeLinear(len, 8))
          case 0x03 => ulongL(16).decode(head.remainder).flatMap(len => decodeLinear(len, 16))
          case 0x04 => ulongL(32).decode(head.remainder).flatMap(len => decodeLinear(len, 32))
          case 0x05 => longL(64).decode(head.remainder).flatMap(len => decodeLinear(len, 64))
          case _ => Attempt.failure(Err("not a vpack array"))
        }
      } yield res
    }
  }

  def main(args: Array[String]): Unit = {
    val ib = VPackValue.vpInt :: VPackValue.vpBool :: HNil
    val dec = vpArray(ib)

    println(dec.decode(hex"0205003119".bits))
  }
}