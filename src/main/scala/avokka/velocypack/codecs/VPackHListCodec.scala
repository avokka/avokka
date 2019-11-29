package avokka.velocypack.codecs

import avokka.velocypack.VPackArray
import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import shapeless.{::, HList, HNil, Lazy}

trait VPackHListCodec[A <: HList] {
  def encode(arguments: A): Attempt[Seq[BitVector]]
  def decode(values: Seq[BitVector]): Attempt[A]
}

object VPackHListCodec {

  // def apply[A <: HList](implicit codec: VPackHListCodec[A]): VPackHListCodec[A] = codec

  implicit object hnilCodec extends VPackHListCodec[HNil] {
    override def encode(arguments: HNil): Attempt[Seq[BitVector]] = Attempt.successful(Vector.empty)
    override def decode(values: Seq[BitVector]): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsCodec[T, A <: HList](implicit ev: VPackHListCodec[A], codec: Codec[T]): VPackHListCodec[T :: A] = new VPackHListCodec[T :: A] {
    override def encode(arguments: T :: A): Attempt[Seq[BitVector]] = {
      for {
        rl <- codec.encode(arguments.head)
        rr <- ev.encode(arguments.tail)
      } yield rl +: rr
    }
    override def decode(values: Seq[BitVector]): Attempt[T :: A] = {
      values match {
        case value +: tail => for {
          rl <- codec.decode(value).map(_.value)
          rr <- ev.decode(tail)
        } yield rl :: rr

        case _ => Attempt.failure(Err("not enough elements in vpack array"))
      }
    }
  }

  def encoder[A <: HList](implicit ev: VPackHListCodec[A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(value)
      arr    <- VPackArrayCodec.encode(VPackArray(values))
    } yield arr
  }

  def encoderCompact[A <: HList](implicit ev: VPackHListCodec[A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(value)
      arr    <- VPackArrayCodec.Compact.encode(VPackArray(values))
    } yield arr
  }

  def decoder[A <: HList](implicit ev: VPackHListCodec[A]): Decoder[A] = Decoder { bits =>
    for {
      arr <- VPackArrayCodec.decode(bits)
      res <- ev.decode(arr.value.values)
    } yield DecodeResult(res, arr.remainder)
  }

  def codec[A <: HList](implicit ev: VPackHListCodec[A]): Codec[A] = Codec(encoder(ev), decoder(ev))
  def codecCompact[A <: HList](implicit ev: VPackHListCodec[A]): Codec[A] = Codec(encoderCompact(ev), decoder(ev))

}
