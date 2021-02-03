package avokka.velocypack

import cats.syntax.all._
import shapeless.labelled.{FieldType, field}
import shapeless.{::, Default, HList, HNil, LabelledGeneric, Witness}
import VPack.VObject
import cats.data.Kleisli
import cats.{Monad, MonadError, MonadThrow}

object VPackRecord {

  private[velocypack] trait Encoder[A <: HList] {
    def encode(t: A): Map[String, VPack]
  }

  private[velocypack] object Encoder {

    def apply[A <: HList](compact: Boolean = false)(
        implicit ev: Encoder[A]
    ): VPackEncoder[A] = value => VObject(ev.encode(value))

    implicit object hnilEncoder extends Encoder[HNil] {
      override def encode(t: HNil): Map[String, VPack] = Map.empty
    }

    implicit def hconsEncoder[K <: Symbol, H, T <: HList](
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

  private[velocypack] trait Decoder[F[_], A <: HList, D <: HList] {
    def decode(v: Map[String, VPack], defaults: D): F[A]
  }

  private[velocypack] object Decoder {
    def apply[F[_], A <: HList, D <: HList](defaults: D)(implicit ev: Decoder[F, A, D], F: MonadThrow[F]): VPackDecoderF[F, A] = Kleisli {
      case VObject(values) => ev.decode(values, defaults)
      case v               => F.raiseError(VPackError.WrongType(v))
    }

    implicit def hnilDecoder[F[_]](implicit F: MonadThrow[F]): Decoder[F, HNil, HNil] = new Decoder[F, HNil, HNil] {
      override def decode(v: Map[String, VPack], defaults: HNil): F[HNil] = F.pure(HNil)
    }

    implicit def hconsDecoder[F[_], K <: Symbol, H, T <: HList](
        implicit ev: Decoder[F, T, HNil],
        key: Witness.Aux[K],
        decoder: VPackDecoderF[F, H],
        F: MonadThrow[F]
    ): Decoder[F, FieldType[K, H] :: T, HNil] = new Decoder[F, FieldType[K, H] :: T, HNil] {
      private val keyName: String = key.value.name

      override def decode(v: Map[String, VPack], defaults: HNil): F[FieldType[K, H] :: T] = {
        v.get(keyName) match {
          case Some(value) =>
            for {
              rl <- decoder(value).adaptErr {
                case e: VPackError => e.historyAdd(keyName)
              }
              rr <- ev.decode(v, HNil)
            } yield field[K](rl) :: rr

          case _ => F.raiseError(VPackError.ObjectFieldAbsent(keyName))
        }
      }
    }

    implicit def hconsDefaultsDecoder[F[_], K <: Symbol, H, T <: HList, D <: HList](
        implicit ev: Decoder[F, T, D],
        key: Witness.Aux[K],
        decoder: VPackDecoderF[F, H],
        F: MonadThrow[F]
    ): Decoder[F, FieldType[K, H] :: T, Option[H] :: D] =
      new Decoder[F, FieldType[K, H] :: T, Option[H] :: D] {
        private val keyName: String = key.value.name

        override def decode(v: Map[String, VPack],
                            defaults: Option[H] :: D): F[FieldType[K, H] :: T] = {
          val default = defaults.head
          v.get(keyName) match {
            case Some(value) =>
              for {
                rl <- decoder(value).adaptErr { case e: VPackError => e.historyAdd(keyName) }
                rr <- ev.decode(v, defaults.tail)
              } yield field[K](rl) :: rr

            case _ if default.isDefined =>
              for {
                rr <- ev.decode(v, defaults.tail)
              } yield field[K](default.get) :: rr

            case _ => F.raiseError(VPackError.ObjectFieldAbsent(keyName))
          }
        }
      }
  }

  private[velocypack] final class DeriveHelper[F[_], T](private val dummy: Boolean = false) extends AnyVal {
    
    def encoder[R <: HList](
        implicit lgen: LabelledGeneric.Aux[T, R],
        e: Encoder[R]
    ): VPackEncoder[T] = Encoder()(e).contramap(lgen.to)

    def decoder[R <: HList](
        implicit lgen: LabelledGeneric.Aux[T, R],
        d: Decoder[F, R, HNil],
        F: MonadThrow[F]
    ): VPackDecoderF[F, T] = Decoder[F, R, HNil](HNil)(d, F).map(lgen.from)

    def decoderWithDefaults[R <: HList, D <: HList](
        implicit lgen: LabelledGeneric.Aux[T, R],
        defaults: Default.AsOptions.Aux[T, D],
        d: Decoder[F, R, D],
        F: MonadThrow[F]
    ): VPackDecoderF[F, T] = Decoder(defaults())(d, F).map(lgen.from)

  }

  def F[F[_], T] = new DeriveHelper[F, T]
  def apply[T] = new DeriveHelper[Result, T]
}
