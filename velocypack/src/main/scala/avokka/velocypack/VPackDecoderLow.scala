package avokka.velocypack

trait VPackDecoderLow {
  implicit def derivedDecoder[T](implicit derived: auto.Derived[VPackDecoder[T]]): VPackDecoder[T] = derived.value
}
