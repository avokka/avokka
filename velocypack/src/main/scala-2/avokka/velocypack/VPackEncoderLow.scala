package avokka.velocypack

trait VPackEncoderLow {
  implicit def derivedEncoder[T](implicit derived: Derived[VPackEncoder[T]]): VPackEncoder[T] = derived.value
}
