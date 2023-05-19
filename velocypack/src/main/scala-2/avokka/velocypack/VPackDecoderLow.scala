package avokka.velocypack

import auto.Derived

trait VPackDecoderLow {
  implicit def derivedDecoder[T](implicit derived: Derived[VPackDecoder[T]]): VPackDecoder[T] = derived.value
}
