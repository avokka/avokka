package avokka.velocypack

import avokka.velocypack.codecs.VPackArrayCodec
import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import shapeless.{::, Generic, HList, HNil}

trait VPackGeneric[A <: HList] {
  def encode(arguments: A): Attempt[Seq[BitVector]]
  def decode(values: Seq[BitVector]): Attempt[A]
}

object VPackGeneric {

  implicit object hnilCodec extends VPackGeneric[HNil] {
    override def encode(arguments: HNil): Attempt[Seq[BitVector]] = Attempt.successful(Vector.empty)
    override def decode(values: Seq[BitVector]): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsCodec[T, A <: HList](implicit codec: Codec[T], ev: VPackGeneric[A]): VPackGeneric[T :: A] = new VPackGeneric[T :: A] {

    override def encode(arguments: T :: A): Attempt[Seq[BitVector]] = for {
      rl <- codec.encode(arguments.head)
      rr <- ev.encode(arguments.tail)
    } yield rl +: rr

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

  def encoder[A <: HList](implicit ev: VPackGeneric[A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(value)
      arr    <- VPackArrayCodec.encode(VPackArray(values))
    } yield arr
  }

  def encoderCompact[A <: HList](implicit ev: VPackGeneric[A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(value)
      arr    <- VPackArrayCodec.Compact.encode(VPackArray(values))
    } yield arr
  }

  def decoder[A <: HList](implicit ev: VPackGeneric[A]): Decoder[A] = Decoder { bits =>
    for {
      arr <- VPackArrayCodec.decode(bits)
      res <- ev.decode(arr.value.values)
    } yield DecodeResult(res, arr.remainder)
  }

  def codec[A <: HList](implicit ev: VPackGeneric[A]): Codec[A] = Codec(encoder(ev), decoder(ev))
  def codecCompact[A <: HList](implicit ev: VPackGeneric[A]): Codec[A] = Codec(encoderCompact(ev), decoder(ev))

  class DeriveHelper[T] {

    def codec[R <: HList](implicit gen: Generic.Aux[T, R], vp: VPackGeneric[R]): Codec[T] = {
      VPackGeneric.codec(vp).xmap(a => gen.from(a), a => gen.to(a))
    }

    def codecCompact[R <: HList](implicit gen: Generic.Aux[T, R], vp: VPackGeneric[R]): Codec[T] = {
      VPackGeneric.codecCompact(vp).xmap(a => gen.from(a), a => gen.to(a))
    }
  }

  def apply[T] = new DeriveHelper[T]
}
