package avokka.velocypack

import avokka.velocypack.VPack.VArray
import avokka.velocypack.codecs.VPackArrayCodec
import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import shapeless.{::, Generic, HList, HNil}
import cats.implicits._

trait VPackGeneric[A <: HList] extends VPackCodec[A] {
  def encode(t: A): VArray
  def decode(values: VPack): VPackDecoder.Result[A]
}

object VPackGeneric {

  implicit object hnilCodec extends VPackGeneric[HNil] {
    override def encode(t: HNil): VArray = VArray()
    override def decode(values: VPack): VPackDecoder.Result[HNil] = HNil.asRight
  }

  implicit def hconsCodec[T, A <: HList](implicit codec: VPackCodec[T], ev: VPackGeneric[A]): VPackGeneric[T :: A] = new VPackGeneric[T :: A] {

    override def encode(arguments: T :: A): VArray = {
      VArray(codec.encode(arguments.head) +: ev.encode(arguments.tail).values)
    }

    override def decode(values: VPack): VPackDecoder.Result[T :: A] = {
      values match {
        case VArray(values) => values match {
          case value +: tail => for {
            rl <- codec.decode (value)
            rr <- ev.decode(VArray(tail))
          } yield rl :: rr

          case _ => VPackError.NotEnoughElements.asLeft // Attempt.failure(Err("not enough elements in vpack array"))
        }
        case _ => VPackError.WrongType.asLeft
      }
    }
  }

  /*
  def encoder[A <: HList](compact: Boolean = false)(implicit ev: VPackGeneric[A]): Encoder[A] = Encoder { value =>
    val values = ev.encode(value)
    val aEncoder = if (compact) VPackArrayCodec.encoderCompact else VPackArrayCodec.encoder
    aEncoder.encode(VArray(values))
  }

  def decoder[A <: HList](implicit ev: VPackGeneric[A]): Decoder[A] = Decoder { bits =>
    for {
      arr <- VPackArrayCodec.decode(bits)
      res <- ev.decode(arr.value.values)
    } yield DecodeResult(res, arr.remainder)
  }

  def codec[A <: HList](compact: Boolean = false)(implicit ev: VPackGeneric[A]): Codec[A] = Codec(
    encoder(compact)(ev),
    decoder(ev)
  )
*/

  class DeriveHelper[T] {

    def codec[R <: HList](implicit gen: Generic.Aux[T, R], vp: VPackGeneric[R]): VPackCodec[T] = {
      vp(a => gen.from(a), a => gen.to(a))
    }

  }

  def apply[T] = new DeriveHelper[T]

}

