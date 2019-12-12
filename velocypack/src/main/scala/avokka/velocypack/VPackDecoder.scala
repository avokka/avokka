package avokka.velocypack

trait VPackDecoder[T] {
  def decode(v: VPack): Either[VPackError, T]
}

object VPackDecoder {

}