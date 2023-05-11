package avokka.velocypack
package auto

object encoder extends VPackEncoderDerivation {
  implicit def derivedVPackEncoder[T]: Derived[VPackEncoder[T]] = macro MagnoliaDerivedMacro.derivedGen[T]
}
