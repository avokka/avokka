package avokka.velocypack.codecs

import avokka.velocypack.{VPackObject, VPackValue}
import scodec.bits.{BitVector, HexStringSyntax}
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist.Align
import shapeless.{::, HList, HNil, LabelledGeneric, Witness}
import shapeless.syntax.singleton._

trait VPackGenericCodec[A <: HList] {
  def encode(arguments: A): Attempt[Map[String, BitVector]]
  def decode(values: Map[String, BitVector]): Attempt[A]
}

object VPackGenericCodec {

//  def apply[E <: HList, A <: HList](implicit encoders: VelocypackArrayEncoder[E, A]): VelocypackArrayEncoder[E, A] = encoders

  implicit object hnilCodec extends VPackGenericCodec[HNil] {
    override def encode(arguments: HNil): Attempt[Map[String, BitVector]] = Attempt.successful(Map.empty)
    override def decode(values: Map[String, BitVector]): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsCodec[K <: Symbol, T, A <: HList]
  (
    implicit ev: VPackGenericCodec[A],
    key: Witness.Aux[K],
    codec: Codec[T]
  ): VPackGenericCodec[FieldType[K, T] :: A] = new VPackGenericCodec[FieldType[K, T] :: A] {
    override def encode(arguments: FieldType[K, T] :: A): Attempt[Map[String, BitVector]] = {
      for {
        rl <- codec.encode(arguments.head)
        rr <- ev.encode(arguments.tail)
      } yield rr.updated(key.value.name, rl)
    }
    override def decode(values: Map[String, BitVector]): Attempt[FieldType[K, T] :: A] = {
      val keyName = key.value.name
      values.get(keyName) match {
        case Some(value) => for {
          rl <- codec.decode(value).map(_.value)
          rr <- ev.decode(values)
        } yield field[K](rl) :: rr

        case _ => Attempt.failure(Err(s"no field $keyName in vpack object"))
      }
    }
  }

  def encoder[A <: HList](implicit ev: VPackGenericCodec[A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(value)
      arr    <- VPackObjectCodec.encode(VPackObject(values))
    } yield arr
  }

  def encoderCompact[A <: HList](implicit ev: VPackGenericCodec[A]): Encoder[A] = Encoder { value =>
    for {
      values <- ev.encode(value)
      arr    <- VPackObjectCodec.Compact.encode(VPackObject(values))
    } yield arr
  }

  def decoder[A <: HList](implicit ev: VPackGenericCodec[A]): Decoder[A] = Decoder { bits =>
    for {
      arr <- VPackObjectCodec.decode(bits)
      res <- ev.decode(arr.value.values)
    } yield DecodeResult(res, arr.remainder)
  }

  def codec[A <: HList](implicit ev: VPackGenericCodec[A]): Codec[A] = Codec(encoder(ev), decoder(ev))
  def codecCompact[A <: HList](implicit ev: VPackGenericCodec[A]): Codec[A] = Codec(encoderCompact(ev), decoder(ev))

  class DeriveHelper[T] {
    def generic[Repr <: HList]
    (
      implicit lgen: LabelledGeneric.Aux[T, Repr],
      reprCodec: VPackGenericCodec[Repr],
    ): Codec[T] = {
      codec[Repr].xmap(a => lgen.from(a), a => lgen.to(a))
    }
  }

  def deriveFor[T] = new DeriveHelper[T]
}
