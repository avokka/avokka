package avokka.velocypack

import magnolia._

trait VPackEncoderDerivation {
  type Typeclass[T] = VPackEncoder[T]

  def combine[T](ctx: CaseClass[VPackEncoder, T]): VPackEncoder[T] = new VPackEncoder[T] {
    override def encode(value: T): VPack = VObject(ctx.parameters.map { p =>
      p.label -> p.typeclass.encode(p.dereference(value))
    }.toMap)
  }

  def dispatch[T](ctx: SealedTrait[VPackEncoder, T]): VPackEncoder[T] = new VPackEncoder[T] {
    override def encode(value: T): VPack = ctx.dispatch(value) { sub =>
      sub.typeclass.encode(sub.cast(value))
    }
  }
}
