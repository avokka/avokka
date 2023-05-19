package avokka.velocypack

import magnolia._
import shapeless.HList

trait VPackDecoderGeneric {
  implicit def genericDecoder[T <: HList](implicit a: VPackGeneric.Decoder[T]): VPackDecoder[T] =
    VPackGeneric.Decoder(a)
    
  // semi auto derivation
  def derived[T]: VPackDecoder[T] = macro Magnolia.gen[T]

}