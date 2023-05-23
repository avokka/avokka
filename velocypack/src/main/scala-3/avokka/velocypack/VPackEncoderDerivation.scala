package avokka.velocypack

import magnolia1._

trait VPackEncoderDerivation extends Derivation[VPackEncoder] {

  override def join[T](ctx: CaseClass[VPackEncoder, T]): VPackEncoder[T] = new VPackEncoder[T] {
    override def encode(value: T): VPack = VObject(ctx.params.map { p =>
      p.label -> p.typeclass.encode(p.deref(value))
    }.toMap)
  }

  override def split[T](ctx: SealedTrait[VPackEncoder, T]): VPackEncoder[T] = new VPackEncoder[T] {
    override def encode(value: T): VPack = ctx.choose(value) { sub =>
      sub.typeclass.encode(sub.cast(value))
    }
  }
}