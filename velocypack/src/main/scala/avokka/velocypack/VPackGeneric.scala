package avokka.velocypack

import avokka.velocypack.VPack.VArray
import cats.data.Chain
import cats.syntax.either._
import cats.syntax.contravariant._
import shapeless.{::, Generic, HList, HNil}

object VPackGeneric { c =>
  import Chain._

  trait Encoder[A <: HList] {
    def encode(t: A): Chain[VPack]
  }

  object Encoder {

    def apply[A <: HList](compact: Boolean = false)(implicit ev: Encoder[A]): VPackEncoder[A] = { value =>
      VArray(ev.encode(value))
    }

    implicit object hnilEncoder extends Encoder[HNil] {
      override def encode(t: HNil): Chain[VPack] = Chain.empty
    }

    implicit def hconsEncoder[T, A <: HList]
    (
      implicit encoder: VPackEncoder[T],
      ev: Encoder[A]
    ): Encoder[T :: A] = (t: T :: A) => {
      encoder.encode(t.head) +: ev.encode(t.tail)
    }
  }

  trait Decoder[A <: HList] {
    def decode(v: Chain[VPack]): VPackDecoder.Result[A]
  }

  object Decoder {
    def apply[A <: HList](implicit ev: Decoder[A]): VPackDecoder[A] = {
      case VArray(values) => ev.decode(values)
      case v => VPackError.WrongType(v).asLeft
    }

    implicit object hnilDecoder extends Decoder[HNil] {
      override def decode(v: Chain[VPack]): VPackDecoder.Result[HNil] = HNil.asRight
    }

    implicit def hconsDecoder[T, A <: HList]
    (
      implicit decoder: VPackDecoder[T], ev: Decoder[A]
    ): Decoder[T :: A] = {
      case value ==: tail => for {
        rl <- decoder.decode(value)
        rr <- ev.decode(tail)
      } yield rl :: rr

      case _ => VPackError.NotEnoughElements.asLeft // Attempt.failure(Err("not enough elements in vpack array"))
    }
  }

  class DeriveHelper[T] {

    def encoder[R <: HList](implicit gen: Generic.Aux[T, R], vp: Encoder[R]): VPackEncoder[T] =
      Encoder()(vp).contramap(gen.to)

    def decoder[R <: HList](implicit gen: Generic.Aux[T, R], vp: Decoder[R]): VPackDecoder[T] =
      Decoder(vp).map(gen.from)

  }

  def apply[T] = new DeriveHelper[T]

}

