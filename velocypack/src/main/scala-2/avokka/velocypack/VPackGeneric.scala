package avokka.velocypack

import shapeless.{::, Generic, HList, HNil}

object VPackGeneric { c =>

  trait Encoder[A <: HList] {
    def encode(t: A): Vector[VPack]
  }

  object Encoder {

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

  trait Decoder[A <: HList] {
    def decode(v: Vector[VPack]): VPackResult[A]
  }

  object Decoder {
    def apply[A <: HList](implicit ev: Decoder[A]): VPackDecoder[A] = {
      case VArray(values) => ev.decode(values)
      case v              => Left(VPackError.WrongType(v))
    }

    implicit val hnilDecoder: Decoder[HNil] = new Decoder[HNil] {
      override def decode(v: Vector[VPack]): VPackResult[HNil] = Right(HNil)
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

      case _ => Left(VPackError.NotEnoughElements())
    }
  }

  final class DeriveHelper[T](private val dummy: Boolean = false) extends AnyVal {

    def cmap[R <: HList](gen: T => R)(implicit vp: Encoder[R]): VPackEncoder[T] =
      Encoder(vp).contramap(gen)

    def map[R <: HList](f: R => T)(implicit vp: Decoder[R]): VPackDecoder[T] =
      Decoder(vp).map(f)

    def encoder[R <: HList](implicit gen: Generic.Aux[T, R], vp: Encoder[R]): VPackEncoder[T] =
      Encoder(vp).contramap(gen.to)

    def decoder[R <: HList](implicit gen: Generic.Aux[T, R], vp: Decoder[R]): VPackDecoder[T] =
      Decoder(vp).map(gen.from)

  }

  def apply[T] = new DeriveHelper[T]

}
