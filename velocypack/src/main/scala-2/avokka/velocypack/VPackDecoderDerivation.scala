package avokka.velocypack

import magnolia1._
import cats.syntax.either._
import scala.language.experimental.macros

trait VPackDecoderDerivation {

  type Typeclass[T] = VPackDecoder[T]

  def join[T](ctx: CaseClass[VPackDecoder, T]): VPackDecoder[T] = new VPackDecoder[T] {
    override def decode(v: VPack): VPackResult[T] = v match {
      case VObject(values) => ctx.constructMonadic { p =>
        values.get(p.label) match {
          case Some(value) => p.typeclass.decode(value).leftMap(_.historyAdd(p.label))
          case None => p.default.toRight(VPackError.ObjectFieldAbsent(p.label))
        }
      }
      case _ => Left(VPackError.WrongType(v))
    }
  }

  def split[T](ctx: SealedTrait[VPackDecoder, T]): VPackDecoder[T] = new VPackDecoder[T] {
    override def decode(v: VPack): VPackResult[T] = {
      val sub = ctx.subtypes.head
      sub.typeclass.decode(v)
    }
  }

  implicit def derived[T]: VPackDecoder[T] = macro Magnolia.gen[T]

}
