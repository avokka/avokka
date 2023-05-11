package avokka.velocypack

trait VPackEncoderGeneric {
  implicit def genericEncoder[T <: HList](implicit a: VPackGeneric.Encoder[T]): VPackEncoder[T] =
    VPackGeneric.Encoder(a)

  // semi auto derivation
  def gen[T]: VPackEncoder[T] = macro Magnolia.gen[T]
}