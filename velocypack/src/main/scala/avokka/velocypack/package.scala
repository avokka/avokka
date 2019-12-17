package avokka

package object velocypack extends ShowInstances {

  implicit class SyntaxToVPack[T](v: T) {
    def toVPack(implicit encoder: VPackEncoder[T]): VPack = encoder.encode(v)
  }

  implicit class SyntaxFromVPack(v: VPack) {
    def as[T](implicit decoder: VPackDecoder[T]): VPackDecoder.Result[T] = decoder.decode(v)
  }
}
