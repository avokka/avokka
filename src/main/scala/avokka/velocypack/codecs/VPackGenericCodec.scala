package avokka.velocypack.codecs

import avokka.velocypack.{VPackObject, VPackValue}
import scodec.bits.{BitVector, HexStringSyntax}
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist.Align
import shapeless.{::, HList, HNil, LabelledGeneric, Witness}
import shapeless.syntax.singleton._

trait VPackGenericCodec[C <: HList, A <: HList] {
  def encode(encoders: C, arguments: A): Attempt[Map[String, BitVector]]
  def decode(decoders: C, values: Map[String, BitVector]): Attempt[A]
}

object VPackGenericCodec {

//  def apply[E <: HList, A <: HList](implicit encoders: VelocypackArrayEncoder[E, A]): VelocypackArrayEncoder[E, A] = encoders

  implicit object hnilCodec extends VPackGenericCodec[HNil, HNil] {
    override def encode(encoders: HNil, arguments: HNil): Attempt[Map[String, BitVector]] = Attempt.successful(Map.empty)
    override def decode(decoders: HNil, values: Map[String, BitVector]): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsCodec[K <: Symbol, T, Cod, C <: HList, A <: HList]
  (
    implicit ev: VPackGenericCodec[C, A],
    key: Witness.Aux[K],
    eve: Cod <:< Codec[T]
  ): VPackGenericCodec[FieldType[K, Cod] :: C, FieldType[K, T] :: A] = new VPackGenericCodec[FieldType[K, Cod] :: C, FieldType[K, T] :: A] {
    override def encode(encoders: FieldType[K, Cod] :: C, arguments: FieldType[K, T] :: A): Attempt[Map[String, BitVector]] = {
      for {
        rl <- encoders.head.encode(arguments.head)
        rr <- ev.encode(encoders.tail, arguments.tail)
      } yield rr.updated(key.value.name, rl)
    }
    override def decode(decoders: FieldType[K, Cod] :: C, values: Map[String, BitVector]): Attempt[FieldType[K, T] :: A] = {
      val keyName = key.value.name
      values.get(keyName) match {
        case Some(value) => for {
          rl <- decoders.head.decode(value).map(_.value)
          rr <- ev.decode(decoders.tail, values)
        } yield field[K](rl) :: rr

        case _ => Attempt.failure(Err(s"no field $keyName in vpack object"))
      }
    }
  }

  def encoder[E <: HList, A <: HList](encoders: E)(implicit ev: VPackGenericCodec[E, A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(encoders, value)
      arr    <- VPackObjectCodec.encode(VPackObject(values))
    } yield arr
  }

  def encoderCompact[E <: HList, A <: HList](encoders: E)(implicit ev: VPackGenericCodec[E, A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(encoders, value)
      arr    <- VPackObjectCodec.Compact.encode(VPackObject(values))
    } yield arr
  }

  def decoder[D <: HList, A <: HList](decoders: D)(implicit ev: VPackGenericCodec[D, A]): Decoder[A] = Decoder { bits =>
    for {
      arr <- VPackObjectCodec.decode(bits)
      res <- ev.decode(decoders, arr.value.values)
    } yield DecodeResult(res, arr.remainder)
  }

  def codec[C <: HList, A <: HList](codecs: C)(implicit ev: VPackGenericCodec[C, A]): Codec[A] = Codec(encoder(codecs), decoder(codecs))
  def codecCompact[C <: HList, A <: HList](codecs: C)(implicit ev: VPackGenericCodec[C, A]): Codec[A] = Codec(encoderCompact(codecs), decoder(codecs))

  class DeriveHelper[T] {
    def apply[Repr <: HList, C <: HList, A <: HList](c: C)
    (
      implicit lgen: LabelledGeneric.Aux[T, Repr],
      reprCodec: VPackGenericCodec[C, A],
      align: Align[Repr, A],
      alignR: Align[A, Repr]
    ): Codec[T] = {
      codec(c).xmap(a => lgen.from(a), a => lgen.to(a))
    }
  }

  def deriveFor[T] = new DeriveHelper[T]

  def main(args: Array[String]): Unit = {

  }

}
