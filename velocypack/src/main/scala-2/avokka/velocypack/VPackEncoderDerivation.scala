package avokka.velocypack

import magnolia1._

trait VPackEncoderDerivation {
  type Typeclass[T] = VPackEncoder[T]

  def join[T](ctx: CaseClass[VPackEncoder, T]): VPackEncoder[T] = new VPackEncoder[T] {
    override def encode(t: T): VPack = VObject(ctx.parameters.map { p =>
      p.label -> p.typeclass.encode(p.dereference(t))
    }.toMap)
  }

  def split[T](ctx: SealedTrait[VPackEncoder, T]): VPackEncoder[T] = new VPackEncoder[T] {
    override def encode(t: T): VPack = ctx.split(t) { sub =>
      sub.typeclass.encode(sub.cast(t))
    }
  }

  implicit def derived[T]: VPackEncoder[T] = macro Magnolia.gen[T]
}
