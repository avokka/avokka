package avokka.velocypack

import _root_.enumeratum._
import cats.syntax.either._

package object enumeratum {

  implicit def enumeratumVPackEncoder[T <: EnumEntry]: VPackEncoder[T] =
    VPackEncoder[String].contramap(_.entryName)

  implicit def enumeratumVPackDecoder[T <: EnumEntry](implicit E: Enum[T]): VPackDecoder[T] =
    VPackDecoder[String].flatMap { name =>
      E.withNameEither(name).leftMap(e => VPackError.IllegalValue(e.getMessage()))
    }
}
