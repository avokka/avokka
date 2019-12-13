package avokka.velocypack

import avokka.velocypack.VPack.VObject
import cats.syntax.either._
import shapeless.labelled.{FieldType, field}
import shapeless.{::, Default, HList, HNil, LabelledGeneric, Witness}

trait VPackRecord[A <: HList, D <: HList] {
  def decode(v: Map[String, VPack], defaults: D): VPackDecoder.Result[A]
}

object VPackRecord { c =>

  trait Encoder[A <: HList] {
    def encode(t: A): Map[String, VPack]
  }

  object Encoder {

    def apply[A <: HList](compact: Boolean = false)(implicit ev: Encoder[A]): VPackEncoder[A] = { value =>
      VObject(ev.encode(value))
    }

    implicit object hnilEncoder extends Encoder[HNil] {
      override def encode(t: HNil): Map[String, VPack] = Map.empty
    }

    implicit def hconsEncoder[K <: Symbol, T, A <: HList]
    (
      implicit ev: Encoder[A],
      key: Witness.Aux[K],
      encoder: VPackEncoder[T],
    ): Encoder[FieldType[K, T] :: A] = new Encoder[FieldType[K, T] :: A] {
      private val keyName: String = key.value.name

      override def encode(t: FieldType[K, T] :: A): Map[String, VPack] = {
        ev.encode(t.tail).updated(keyName, encoder.encode(t.head))
      }
    }
  }

  implicit object hnilCodec extends VPackRecord[HNil, HNil] {
    override def decode(v: Map[String, VPack], defaults: HNil): VPackDecoder.Result[HNil] = HNil.asRight
  }

  implicit def hconsCodec[K <: Symbol, T, A <: HList]
  (
    implicit ev: VPackRecord[A, HNil],
    key: Witness.Aux[K],
    decoder: VPackDecoder[T]
  ): VPackRecord[FieldType[K, T] :: A, HNil] = new VPackRecord[FieldType[K, T] :: A, HNil] {
    private val keyName: String = key.value.name

    override def decode(v: Map[String, VPack], defaults: HNil): VPackDecoder.Result[FieldType[K, T] :: A] = {
      v.get(keyName) match {
        case Some(value) => for {
          rl <- decoder.decode(value) //.mapErr(_.pushContext(keyName))
          rr <- ev.decode(v, HNil)
        } yield field[K](rl) :: rr

        case _ => VPackError.ObjectFieldAbsent(keyName).asLeft // Attempt.failure(Err(s"no field $keyName in vpack object"))
      }
    }
  }

  implicit def hconsDefaultsCodec[K <: Symbol, T, A <: HList, D <: HList]
  (
    implicit ev: VPackRecord[A, D],
    key: Witness.Aux[K],
    decoder: VPackDecoder[T]
  ): VPackRecord[FieldType[K, T] :: A, Option[T] :: D] = new VPackRecord[FieldType[K, T] :: A, Option[T] :: D] {
    private val keyName: String = key.value.name

    override def decode(v: Map[String, VPack], defaults: Option[T] :: D): VPackDecoder.Result[FieldType[K, T] :: A] = {
      val default = defaults.head
      v.get(keyName) match {
        case Some(value) => for {
          rl <- decoder.decode(value) //.mapErr(_.pushContext(keyName))
          rr <- ev.decode(v, defaults.tail)
        } yield field[K](rl) :: rr

        case _ if default.isDefined => for {
          rr <- ev.decode(v, defaults.tail)
        } yield field[K](default.get) :: rr

        case _ => VPackError.ObjectFieldAbsent(keyName).asLeft // Attempt.failure(Err(s"no field $keyName in vpack object"))
      }
    }
  }

  def decoder[A <: HList, D <: HList](defaults: D)(implicit ev: VPackRecord[A, D]): VPackDecoder[A] = {
    case VObject(values) => ev.decode(values, defaults)
    case _ => VPackError.WrongType.asLeft
  }

  class DeriveHelper[T] {
    def encoder[R <: HList]
    (
      implicit lgen: LabelledGeneric.Aux[T, R],
      repr: Encoder[R],
    ): VPackEncoder[T] = {
      Encoder()(repr).contramap(lgen.to) //.xmap(a => lgen.from(a), a => lgen.to(a))
    }

    def decoder[R <: HList]
    (
      implicit lgen: LabelledGeneric.Aux[T, R],
      repr: VPackRecord[R, HNil],
    ): VPackDecoder[T] = {
      c.decoder[R, HNil](HNil)(repr).map(lgen.from) //.xmap(a => lgen.from(a), a => lgen.to(a))
    }

    def decoderWithDefaults[R <: HList, D <: HList]
    (
      implicit lgen: LabelledGeneric.Aux[T, R],
      defaults: Default.AsOptions.Aux[T, D],
      repr: VPackRecord[R, D],
    ): VPackDecoder[T] = {
      c.decoder(defaults())(repr).map(lgen.from)
    }
  }

  def apply[T] = new DeriveHelper[T]
}
