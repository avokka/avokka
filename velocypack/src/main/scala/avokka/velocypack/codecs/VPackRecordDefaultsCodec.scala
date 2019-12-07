package avokka.velocypack.codecs

import avokka.velocypack.VPackObject
import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import shapeless.labelled.{FieldType, field}
import shapeless.{::, Default, HList, HNil, LabelledGeneric, Witness}

trait VPackRecordDefaultsCodec[A <: HList, D <: HList] {
  def encode(arguments: A): Attempt[Map[String, BitVector]]
  def decode(values: Map[String, BitVector], defaults: D): Attempt[A]
}

object VPackRecordDefaultsCodec {

  implicit object hnilCodec extends VPackRecordDefaultsCodec[HNil, HNil] {
    override def encode(arguments: HNil): Attempt[Map[String, BitVector]] = Attempt.successful(Map.empty)
    override def decode(values: Map[String, BitVector], defaults: HNil): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsCodec[K <: Symbol, T, A <: HList, D <: HList]
  (
    implicit ev: VPackRecordDefaultsCodec[A, D],
    key: Witness.Aux[K],
    codec: Codec[T]
  ): VPackRecordDefaultsCodec[FieldType[K, T] :: A, Option[T] :: D] = new VPackRecordDefaultsCodec[FieldType[K, T] :: A, Option[T] :: D] {
    override def encode(arguments: FieldType[K, T] :: A): Attempt[Map[String, BitVector]] = {
      for {
        rl <- codec.encode(arguments.head)
        rr <- ev.encode(arguments.tail)
      } yield rr.updated(key.value.name, rl)
    }
    override def decode(values: Map[String, BitVector], defaults: Option[T] :: D): Attempt[FieldType[K, T] :: A] = {
      val keyName = key.value.name
      val default = defaults.head
      values.get(keyName) match {
        case Some(value) => for {
          rl <- codec.decode(value).map(_.value)
          rr <- ev.decode(values, defaults.tail)
        } yield field[K](rl) :: rr

        case _ if default.isDefined => for {
          rr <- ev.decode(values, defaults.tail)
        } yield field[K](default.get) :: rr

        case _ => Attempt.failure(Err(s"no field $keyName in vpack object"))
      }
    }
  }

  def encoder[A <: HList, D <: HList](compact: Boolean = false)(implicit ev: VPackRecordDefaultsCodec[A, D]): Encoder[A] = Encoder { value =>
    for {
      values   <- ev.encode(value)
      oEncoder = if (compact) VPackObjectCodec.Compact else VPackObjectCodec
      arr      <- oEncoder.encode(VPackObject(values))
    } yield arr
  }

  def decoder[A <: HList, D <: HList](defaults: D)(implicit ev: VPackRecordDefaultsCodec[A, D]): Decoder[A] = Decoder { bits =>
    for {
      arr <- VPackObjectCodec.decode(bits)
      res <- ev.decode(arr.value.values, defaults)
    } yield DecodeResult(res, arr.remainder)
  }

  def codec[A <: HList, D <: HList](defaults: D)(implicit ev: VPackRecordDefaultsCodec[A, D]): Codec[A] = Codec(encoder()(ev), decoder(defaults)(ev))
  def codecCompact[A <: HList, D <: HList](defaults: D)(implicit ev: VPackRecordDefaultsCodec[A, D]): Codec[A] = Codec(encoder(compact = true)(ev), decoder(defaults)(ev))

  class DeriveHelper[T] {
    def codec[R <: HList, D <: HList]
    (
      implicit lgen: LabelledGeneric.Aux[T, R],
      defaults: Default.AsOptions.Aux[T, D],
      reprCodec: VPackRecordDefaultsCodec[R, D],
    ): Codec[T] = {
      VPackRecordDefaultsCodec.codec[R, D](defaults()).xmap(a => lgen.from(a), a => lgen.to(a))
    }
  }

  def apply[T] = new DeriveHelper[T]
}
