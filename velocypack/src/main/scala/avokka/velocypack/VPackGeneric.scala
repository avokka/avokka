package avokka.velocypack

import cats.syntax.either._
import shapeless.{::, Generic, HList, HNil}
import VPack.VArray

object VPackGeneric { c =>

  private[velocypack] trait Encoder[A <: HList] {
    def encode(t: A): Vector[VPack]
  }

  private[velocypack] object Encoder {

    def apply[A <: HList](compact: Boolean = false)(
      implicit ev: Encoder[A]
    ): VPackEncoder[A] = value => VArray(ev.encode(value))


    implicit object hnilEncoder extends Encoder[HNil] {
      override def encode(t: HNil): Vector[VPack] = Vector.empty
    }

    implicit def hconsEncoder[T, A <: HList](
        implicit encoder: VPackEncoder[T],
        ev: Encoder[A]
    ): Encoder[T :: A] = (t: T :: A) => encoder.encode(t.head) +: ev.encode(t.tail)

  }

  private[velocypack] trait Decoder[A <: HList] {
    def decode(v: Vector[VPack]): Result[A]
  }

  private[velocypack] object Decoder {
    def apply[A <: HList](implicit ev: Decoder[A]): VPackDecoder[A] = {
      case VArray(values) => ev.decode(values)
      case v              => VPackError.WrongType(v).asLeft
    }

    implicit object hnilDecoder extends Decoder[HNil] {
      override def decode(v: Vector[VPack]): Result[HNil] = HNil.asRight
    }

    implicit def hconsDecoder[T, A <: HList](
        implicit decoder: VPackDecoder[T],
        ev: Decoder[A],
    ): Decoder[T :: A] = {
      case value +: tail =>
        for {
          rl <- decoder.decode(value)
          rr <- ev.decode(tail)
        } yield rl :: rr

      case _ => VPackError.NotEnoughElements().asLeft
    }
  }

  private[velocypack] final class DeriveHelper[T](private val dummy: Boolean = false) extends AnyVal {

    def encoder[R <: HList](implicit gen: Generic.Aux[T, R], vp: Encoder[R]): VPackEncoder[T] =
      Encoder()(vp).contramap(gen.to)

    def decoder[R <: HList](implicit gen: Generic.Aux[T, R], vp: Decoder[R]): VPackDecoder[T] =
      Decoder(vp).map(gen.from)

  }

  def apply[T] = new DeriveHelper[T]

}
