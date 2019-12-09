package avokka.velocypack

import avokka.velocypack.codecs.VPackObjectCodec
import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err}
import shapeless.labelled.{FieldType, field}
import shapeless.{::, Default, HList, HNil, LabelledGeneric, Witness}

trait VPackRecord[A <: HList, D <: HList] {
  def encode(arguments: A): Attempt[Map[String, BitVector]]
  def decode(values: Map[String, BitVector], defaults: D): Attempt[A]
}

object VPackRecord {

  implicit object hnilCodec extends VPackRecord[HNil, HNil] {
    override def encode(arguments: HNil): Attempt[Map[String, BitVector]] = Attempt.successful(Map.empty)
    override def decode(values: Map[String, BitVector], defaults: HNil): Attempt[HNil] = Attempt.successful(HNil)
  }

  implicit def hconsCodec[K <: Symbol, T, A <: HList]
  (
    implicit ev: VPackRecord[A, HNil],
    key: Witness.Aux[K],
    codec: Codec[T]
  ): VPackRecord[FieldType[K, T] :: A, HNil] = new VPackRecord[FieldType[K, T] :: A, HNil] {
    private val keyName: String = key.value.name
    override def encode(arguments: FieldType[K, T] :: A): Attempt[Map[String, BitVector]] = {
      for {
        rl <- codec.encode(arguments.head).mapErr(_.pushContext(keyName))
        rr <- ev.encode(arguments.tail)
      } yield rr.updated(keyName, rl)
    }
    override def decode(values: Map[String, BitVector], defaults: HNil): Attempt[FieldType[K, T] :: A] = {
      values.get(keyName) match {
        case Some(value) => for {
          rl <- codec.decodeValue(value).mapErr(_.pushContext(keyName))
          rr <- ev.decode(values, HNil)
        } yield field[K](rl) :: rr

        case _ => Attempt.failure(Err(s"no field $keyName in vpack object"))
      }
    }
  }

  implicit def hconsDefaultsCodec[K <: Symbol, T, A <: HList, D <: HList]
  (
    implicit ev: VPackRecord[A, D],
    key: Witness.Aux[K],
    codec: Codec[T]
  ): VPackRecord[FieldType[K, T] :: A, Option[T] :: D] = new VPackRecord[FieldType[K, T] :: A, Option[T] :: D] {
    private val keyName: String = key.value.name
    override def encode(arguments: FieldType[K, T] :: A): Attempt[Map[String, BitVector]] = {
      for {
        rl <- codec.encode(arguments.head).mapErr(_.pushContext(keyName))
        rr <- ev.encode(arguments.tail)
      } yield rr.updated(key.value.name, rl)
    }
    override def decode(values: Map[String, BitVector], defaults: Option[T] :: D): Attempt[FieldType[K, T] :: A] = {
      val default = defaults.head
      values.get(keyName) match {
        case Some(value) => for {
          rl <- codec.decodeValue(value).mapErr(_.pushContext(keyName))
          rr <- ev.decode(values, defaults.tail)
        } yield field[K](rl) :: rr

        case _ if default.isDefined => for {
          rr <- ev.decode(values, defaults.tail)
        } yield field[K](default.get) :: rr

        case _ => Attempt.failure(Err(s"no field $keyName in vpack object"))
      }
    }
  }

  def encoder[A <: HList, D <: HList](compact: Boolean = false)(implicit ev: VPackRecord[A, D]): Encoder[A] = Encoder { value =>
    for {
      values   <- ev.encode(value)
      oEncoder = if (compact) VPackObjectCodec.Compact else VPackObjectCodec
      arr      <- oEncoder.encode(VPackObject(values))
    } yield arr
  }

  def decoder[A <: HList, D <: HList](defaults: D)(implicit ev: VPackRecord[A, D]): Decoder[A] = Decoder { bits =>
    for {
      arr <- VPackObjectCodec.decode(bits)
      res <- ev.decode(arr.value.values, defaults)
    } yield DecodeResult(res, arr.remainder)
  }

  def codec[A <: HList, D <: HList](compact: Boolean = false, defaults: D = HNil)
                                   (implicit ev: VPackRecord[A, D]): Codec[A] = Codec(
    encoder(compact)(ev), decoder(defaults)(ev)
  )

  class DeriveHelper[T] {
    def codecWithDefaults[R <: HList, D <: HList]
    (
      implicit lgen: LabelledGeneric.Aux[T, R],
      defaults: Default.AsOptions.Aux[T, D],
      reprCodec: VPackRecord[R, D],
    ): Codec[T] = {
      VPackRecord.codec[R, D](defaults = defaults()).xmap(a => lgen.from(a), a => lgen.to(a))
    }

    def codec[R <: HList]
    (
      implicit lgen: LabelledGeneric.Aux[T, R],
      reprCodec: VPackRecord[R, HNil],
    ): Codec[T] = {
      VPackRecord.codec[R, HNil]().xmap(a => lgen.from(a), a => lgen.to(a))
    }
  }

  def apply[T] = new DeriveHelper[T]
}
