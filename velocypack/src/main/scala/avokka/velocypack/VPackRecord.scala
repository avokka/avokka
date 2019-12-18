package avokka.velocypack

import avokka.velocypack.VPack.VObject
import cats.syntax.either._
import cats.syntax.contravariant._
import shapeless.labelled.{FieldType, field}
import shapeless.{::, Default, HList, HNil, LabelledGeneric, Witness}

object VPackRecord {

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

    implicit def hconsEncoder[K <: Symbol, H, T <: HList]
    (
      implicit ev: Encoder[T],
      key: Witness.Aux[K],
      encoder: VPackEncoder[H],
    ): Encoder[FieldType[K, H] :: T] = new Encoder[FieldType[K, H] :: T] {
      private val keyName: String = key.value.name

      override def encode(t: FieldType[K, H] :: T): Map[String, VPack] = {
        ev.encode(t.tail).updated(keyName, encoder.encode(t.head))
      }
    }
  }

  trait Decoder[A <: HList, D <: HList] {
    def decode(v: Map[String, VPack], defaults: D): Result[A]
  }

  object Decoder {
    def apply[A <: HList, D <: HList](defaults: D)(implicit ev: Decoder[A, D]): VPackDecoder[A] = {
      case VObject(values) => ev.decode(values, defaults)
      case v => VPackError.WrongType(v).asLeft
    }

    implicit object hnilDecoder extends Decoder[HNil, HNil] {
      override def decode(v: Map[String, VPack], defaults: HNil): Result[HNil] = HNil.asRight
    }

    implicit def hconsDecoder[K <: Symbol, H, T <: HList]
    (
      implicit ev: Decoder[T, HNil],
      key: Witness.Aux[K],
      decoder: VPackDecoder[H]
    ): Decoder[FieldType[K, H] :: T, HNil] = new Decoder[FieldType[K, H] :: T, HNil] {
      private val keyName: String = key.value.name

      override def decode(v: Map[String, VPack], defaults: HNil): Result[FieldType[K, H] :: T] = {
        v.get(keyName) match {
          case Some(value) => for {
            rl <- decoder.decode(value) //.mapErr(_.pushContext(keyName))
            rr <- ev.decode(v, HNil)
          } yield field[K](rl) :: rr

          case _ => VPackError.ObjectFieldAbsent(keyName).asLeft
        }
      }
    }

    implicit def hconsDefaultsDecoder[K <: Symbol, H, T <: HList, D <: HList]
    (
      implicit ev: Decoder[T, D],
      key: Witness.Aux[K],
      decoder: VPackDecoder[H]
    ): Decoder[FieldType[K, H] :: T, Option[H] :: D] = new Decoder[FieldType[K, H] :: T, Option[H] :: D] {
      private val keyName: String = key.value.name

      override def decode(v: Map[String, VPack], defaults: Option[H] :: D): Result[FieldType[K, H] :: T] = {
        val default = defaults.head
        v.get(keyName) match {
          case Some(value) => for {
            rl <- decoder.decode(value) //.mapErr(_.pushContext(keyName))
            rr <- ev.decode(v, defaults.tail)
          } yield field[K](rl) :: rr

          case _ if default.isDefined => for {
            rr <- ev.decode(v, defaults.tail)
          } yield field[K](default.get) :: rr

          case _ => VPackError.ObjectFieldAbsent(keyName).asLeft
        }
      }
    }
  }

  class DeriveHelper[T] {
    def encoder[R <: HList](implicit lgen: LabelledGeneric.Aux[T, R], e: Encoder[R]): VPackEncoder[T] = {
      Encoder()(e).contramap(lgen.to)
    }

    def decoder[R <: HList](implicit lgen: LabelledGeneric.Aux[T, R], d: Decoder[R, HNil]): VPackDecoder[T] = {
      Decoder[R, HNil](HNil)(d).map(lgen.from)
    }

    def decoderWithDefaults[R <: HList, D <: HList]
    (
      implicit lgen: LabelledGeneric.Aux[T, R],
      defaults: Default.AsOptions.Aux[T, D],
      d: Decoder[R, D],
    ): VPackDecoder[T] = {
      Decoder(defaults())(d).map(lgen.from)
    }
  }

  def apply[T] = new DeriveHelper[T]
}
