package avokka.velocypack

trait VPackDecoder[T] {
  def decode(v: VPackValue): Either[VPackError, T]
}

object VPackDecoder {

}