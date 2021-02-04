package avokka.velocypack

import avokka.velocypack.VPack.VArray
import cats.MonadThrow
import cats.data.Kleisli
import cats.syntax.all._
import shapeless.{::, Generic, HList, HNil}

object VPackGeneric { c =>

  private[velocypack] trait Encoder[A <: HList] {
    def encode(t: A): Vector[VPack]
  }

  private[velocypack] object Encoder {

    def apply[A <: HList](
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

  trait Decoder[F[_], A <: HList] {
    def decode(v: Vector[VPack]): F[A]
  }

  private[velocypack] object Decoder {
    def apply[F[_], A <: HList](implicit ev: Decoder[F, A], F: MonadThrow[F]): VPackDecoder[F, A] = Kleisli {
      case VArray(values) => ev.decode(values)
      case v              => F.raiseError(VPackError.WrongType(v))
    }

    implicit def hnilDecoder[F[_]](implicit F: MonadThrow[F]): Decoder[F, HNil] = new Decoder[F, HNil] {
      override def decode(v: Vector[VPack]): F[HNil] = F.pure(HNil)
    }

    implicit def hconsDecoder[F[_], T, A <: HList](
        implicit decoder: VPackDecoder[F, T],
        ev: Decoder[F, A],
        F: MonadThrow[F]
    ): Decoder[F, T :: A] = {
      case value +: tail =>
        for {
          rl <- decoder(value)
          rr <- ev.decode(tail)
        } yield rl :: rr

      case _ => F.raiseError(VPackError.NotEnoughElements())
    }
  }

  private[velocypack] final class DeriveHelper[F[_], T](private val dummy: Boolean = false) extends AnyVal {

    def encoder[R <: HList](implicit gen: Generic.Aux[T, R], vp: Encoder[R]): VPackEncoder[T] =
      Encoder(vp).contramap(gen.to)

    def decoder[R <: HList](implicit gen: Generic.Aux[T, R], vp: Decoder[F, R], F: MonadThrow[F]): VPackDecoder[F, T] =
      Decoder(vp, F).map(gen.from)

  }

  def F[F[_], T] = new DeriveHelper[F, T]
  def apply[T] = new DeriveHelper[Result, T]

}
