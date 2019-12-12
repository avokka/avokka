package avokka.velocypack

import avokka.velocypack.VPack.VArray
import avokka.velocypack.codecs.VPackArrayCodec
import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import shapeless.{::, Generic, HList, HNil}
import cats.implicits._

trait VPackGeneric[A <: HList] {
  def encode(arguments: A): Seq[VPack]
  def decode(values: Seq[VPack]): VPackDecoder.Result[A]
}

object VPackGeneric {

  implicit object hnilCodec extends VPackGeneric[HNil] {
    override def encode(arguments: HNil): Seq[VPack] = Vector.empty
    override def decode(values: Seq[VPack]): VPackDecoder.Result[HNil] = HNil.asRight
  }

  implicit def hconsCodec[T, A <: HList](implicit codec: VPackCodec[T], ev: VPackGeneric[A]): VPackGeneric[T :: A] = new VPackGeneric[T :: A] {

    override def encode(arguments: T :: A): Seq[VPack] = {
      codec.encode(arguments.head) +: ev.encode(arguments.tail)
    }

    override def decode(values: Seq[VPack]): VPackDecoder.Result[T :: A] = {
      values match {
        case value +: tail => for {
          rl <- codec.decode(value)
          rr <- ev.decode(tail)
        } yield rl :: rr

        case _ => VPackError.NotEnoughElements.asLeft // Attempt.failure(Err("not enough elements in vpack array"))
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
      vp.xmap(a => gen.from(a), a => gen.to(a))
    }

  }

  def apply[T] = new DeriveHelper[T]

}

