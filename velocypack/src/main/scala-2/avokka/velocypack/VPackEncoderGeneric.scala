package avokka.velocypack

import shapeless.HList

trait VPackEncoderGeneric {
  implicit def genericEncoder[T <: HList](implicit a: VPackGeneric.Encoder[T]): VPackEncoder[T] =
    VPackGeneric.Encoder(a)

}