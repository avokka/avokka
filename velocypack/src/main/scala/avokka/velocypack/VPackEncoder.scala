package avokka.velocypack

trait VPackEncoder[T] {
  def encode(t: T): VPack
}

object VPackEncoder {

}
