package avokka.velocypack

import avokka.velocypack.VPack.VArray
import avokka.velocypack.VPackDecoder.Result
import cats.data.Chain
import cats.implicits._
import shapeless.{::, Generic, HList, HNil}

trait VPackGeneric[A <: HList] {
  def encode(t: A): Chain[VPack]
  def decode(v: Chain[VPack]): VPackDecoder.Result[A]
}

object VPackGeneric {
  import Chain._

  implicit object hnilCodec extends VPackGeneric[HNil] {
    override def encode(t: HNil): Chain[VPack] = Chain.empty
    override def decode(v: Chain[VPack]): VPackDecoder.Result[HNil] = HNil.asRight
  }

  implicit def hconsCodec[T, A <: HList](implicit encoder: VPackEncoder[T], decoder: VPackDecoder[T], ev: VPackGeneric[A]): VPackGeneric[T :: A] = new VPackGeneric[T :: A] {

    override def encode(t: T :: A): Chain[VPack] = {
      encoder.encode(t.head) +: ev.encode(t.tail) //.prepend()
    }

    override def decode(v: Chain[VPack]): VPackDecoder.Result[T :: A] = {
      v match {
        case value ==: tail => for {
          rl <- decoder.decode(value)
          rr <- ev.decode(tail)
        } yield rl :: rr

        case _ => VPackError.NotEnoughElements.asLeft // Attempt.failure(Err("not enough elements in vpack array"))
      }
    }
  }

  def encoder[A <: HList](compact: Boolean = false)(implicit ev: VPackGeneric[A]): VPackEncoder[A] = { value =>
    VArray(ev.encode(value).toVector)
  }

  def decoder[A <: HList](implicit ev: VPackGeneric[A]): VPackDecoder[A] = {
    case VArray(values) => ev.decode(Chain.fromSeq(values))
    case _ => VPackError.WrongType.asLeft
  }

  def codec[A <: HList](compact: Boolean = false)(implicit ev: VPackGeneric[A]): VPackCodec[A] = VPackCodec(
    encoder(compact)(ev),
    decoder(ev)
  )

  class DeriveHelper[T] {

    def codec[R <: HList](implicit gen: Generic.Aux[T, R], vp: VPackGeneric[R]): VPackCodec[T] = VPackCodec(
      encoder()(vp).contramap(gen.to), // VArray(vp.encode(gen.to(t)).toVector)
      decoder(vp).map(gen.from)
    )
    /*
    override def decode(v: VPack): Result[T] = v match {
        case VArray(values) => vp.decode(Chain.fromSeq(values)).map(gen.from)
        case _ => VPackError.WrongType.asLeft
      }
    }*/
  }

  def apply[T] = new DeriveHelper[T]

}

