package avokka

import scodec.bits.BitVector
import scodec.{Attempt, Decoder, Encoder}

package object velocypack extends CodecImplicits {

  implicit class VPackSyntax[T](v: T) {
    def toVPack(implicit encoder: Encoder[T] with Decoder[T]): Attempt[BitVector] = encoder.encode(v)
  }

}
