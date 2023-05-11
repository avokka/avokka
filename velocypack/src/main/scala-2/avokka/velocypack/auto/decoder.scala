package avokka.velocypack
package auto

object decoder extends VPackDecoderDerivation {
  implicit def derivedVPackDecoder[T]: Derived[VPackDecoder[T]] = macro MagnoliaDerivedMacro.derivedGen[T]
}
