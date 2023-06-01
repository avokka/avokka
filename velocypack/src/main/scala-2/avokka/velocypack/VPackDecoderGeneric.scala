package avokka.velocypack

import shapeless.HList

trait VPackDecoderGeneric {
  implicit def genericDecoder[T <: HList](implicit a: VPackGeneric.Decoder[T]): VPackDecoder[T] =
    VPackGeneric.Decoder(a)

}