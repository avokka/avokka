package avokka.velocypack

trait VPackEncoder[T] {
  def encode(t: T): VPackValue
}

object VPackEncoder {

}
