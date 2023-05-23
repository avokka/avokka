package avokka.velocypack

import magnolia1.*
import cats.syntax.either.*

trait VPackDecoderDerivation extends Derivation[VPackDecoder] {
  override def join[T](ctx: CaseClass[VPackDecoder, T]): VPackDecoder[T] = {
    case VObject(values) => ctx.constructMonadic { p =>
      values.get(p.label) match {
        case Some(value) => p.typeclass.decode(value).left.map(_.historyAdd(p.label))
        case None => p.default.toRight(VPackError.ObjectFieldAbsent(p.label))
      }
    }
    case v => Left(VPackError.WrongType(v))
  }

  /** defines how to choose which subtype of the sealed trait to use for
   * decoding
   */
  override def split[T](ctx: SealedTrait[VPackDecoder, T]): VPackDecoder[T] = new VPackDecoder[T] {
    override def decode(v: VPack): VPackResult[T] = {
      val sub = ctx.subtypes.head
      sub.typeclass.decode(v)
    }
  }

}
