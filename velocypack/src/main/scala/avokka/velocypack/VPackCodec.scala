package avokka.velocypack
import avokka.velocypack.VPackDecoder.Result

trait VPackCodec[T] extends VPackDecoder[T] with VPackEncoder[T]

object VPackCodec {
  def apply[T](implicit instance: VPackCodec[T]): VPackCodec[T] = instance

  def apply[T](implicit encoder: VPackEncoder[T], decoder: VPackDecoder[T]): VPackCodec[T] = new VPackCodec[T] {
    override def encode(t: T): VPack = encoder.encode(t)
    override def decode(v: VPack): Result[T] = decoder.decode(v)
  }
}
