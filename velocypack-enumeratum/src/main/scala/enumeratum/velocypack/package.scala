package enumeratum

import avokka.velocypack._
import cats.syntax.either._

package object velocypack {

  implicit def enumeratumVPackEncoder[T <: EnumEntry]: VPackEncoder[T] =
    VPackEncoder[String].contramap(_.entryName)

  implicit def enumeratumVPackDecoder[T <: EnumEntry](implicit E: Enum[T]): VPackDecoder[T] =
    VPackDecoder[String].flatMap { name =>
      E.withNameEither(name).leftMap(e => VPackError.IllegalValue(e.getMessage()))
    }


  implicit def enumeratumVPackKeyEncoder[T <: EnumEntry]: VPackKeyEncoder[T] =
    VPackKeyEncoder[String].contramap(_.entryName)

  implicit def enumeratumVPackKeyDecoder[T <: EnumEntry](implicit E: Enum[T]): VPackKeyDecoder[T] =
    VPackKeyDecoder[String].flatMap { name =>
      E.withNameEither(name).leftMap(e => VPackError.IllegalValue(e.getMessage()))
    }
}
