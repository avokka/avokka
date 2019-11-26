package avokka.velocypack.codecs

import avokka.velocypack.VPackArray
import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import shapeless.{::, HList, HNil}

trait VPackHListCodec[C <: HList, A <: HList] {
  def encode(encoders: C, arguments: A): Attempt[Seq[BitVector]]
  def decode(decoders: C, values: Seq[BitVector]): Attempt[A]
}

object VPackHListCodec {

//  def apply[E <: HList, A <: HList](implicit encoders: VelocypackArrayEncoder[E, A]): VelocypackArrayEncoder[E, A] = encoders

  implicit object hnilCodec extends VPackHListCodec[HNil, HNil] {
    override def encode(encoders: HNil, arguments: HNil): Attempt[Seq[BitVector]] = Attempt.successful(Vector.empty)
    override def decode(decoders: HNil, values: Seq[BitVector]): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsCodec[T, Cod, C <: HList, A <: HList](implicit ev: VPackHListCodec[C, A], eve: Cod <:< Codec[T]): VPackHListCodec[Cod :: C, T :: A] = new VPackHListCodec[Cod :: C, T :: A] {
    override def encode(encoders: Cod :: C, arguments: T :: A): Attempt[Seq[BitVector]] = {
      for {
        rl <- encoders.head.encode(arguments.head)
        rr <- ev.encode(encoders.tail, arguments.tail)
      } yield rl +: rr
    }
    override def decode(decoders: Cod :: C, values: Seq[BitVector]): Attempt[T :: A] = {
      values match {
        case value +: tail => for {
          rl <- decoders.head.decode(value).map(_.value)
          rr <- ev.decode(decoders.tail, tail)
        } yield rl :: rr

        case _ => Attempt.failure(Err("not enough elements in vpack array"))
      }
    }
  }

  def encoder[E <: HList, A <: HList](encoders: E)(implicit ev: VPackHListCodec[E, A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(encoders, value)
      arr    <- VPackArrayCodec.encode(VPackArray(values))
    } yield arr
  }

  def encoderCompact[E <: HList, A <: HList](encoders: E)(implicit ev: VPackHListCodec[E, A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(encoders, value)
      arr    <- VPackArrayCodec.Compact.encode(VPackArray(values))
    } yield arr
  }

  def decoder[D <: HList, A <: HList](decoders: D)(implicit ev: VPackHListCodec[D, A]): Decoder[A] = Decoder { bits =>
    for {
      arr <- VPackArrayCodec.decode(bits)
      res <- ev.decode(decoders, arr.value.values)
    } yield DecodeResult(res, arr.remainder)
  }

  def codec[C <: HList, A <: HList](codecs: C)(implicit ev: VPackHListCodec[C, A]): Codec[A] = Codec(encoder(codecs), decoder(codecs))
  def codecCompact[C <: HList, A <: HList](codecs: C)(implicit ev: VPackHListCodec[C, A]): Codec[A] = Codec(encoderCompact(codecs), decoder(codecs))

}
