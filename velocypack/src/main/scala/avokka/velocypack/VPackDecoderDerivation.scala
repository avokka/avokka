package avokka.velocypack

import magnolia._
import cats.syntax.either._

trait VPackDecoderDerivation {

  type Typeclass[T] = VPackDecoder[T]

  def combine[T](ctx: CaseClass[VPackDecoder, T]): VPackDecoder[T] = new VPackDecoder[T] {
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

  /*
  def dispatch[T](ctx: SealedTrait[VPackDecoder, T]): VPackDecoder[T] =
    new VPackDecoder[T] {
      override def decode(v: VPack): VPackResult[T] = ctx.dispatch(v) { sub =>
        sub.typeclass.decode(sub.cast(v))
      }
    }
*/
}
