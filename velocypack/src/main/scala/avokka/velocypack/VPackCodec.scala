package avokka.velocypack

trait VPackCodec[T] extends VPackDecoder[T] with VPackEncoder[T]

object VPackCodec {
  def apply[T](implicit instance: VPackCodec[T]): VPackCodec[T] = instance

}
