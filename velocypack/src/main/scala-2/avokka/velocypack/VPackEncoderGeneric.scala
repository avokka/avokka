package avokka.velocypack

import shapeless.HList
import magnolia.Magnolia

trait VPackEncoderGeneric {
  implicit def genericEncoder[T <: HList](implicit a: VPackGeneric.Encoder[T]): VPackEncoder[T] =
    VPackGeneric.Encoder(a)

  // semi auto derivation
  def derived[T]: VPackEncoder[T] = macro Magnolia.gen[T]
}