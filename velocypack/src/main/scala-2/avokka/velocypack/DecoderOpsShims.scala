package avokka.velocypack

import scodec.bits.BitVector
import scodec.Decoder

trait DecoderOpsShims[T] {
  def decoder: Decoder[T]

  def collect[F[_], A](buffer: BitVector, limit: Option[Int])(implicit cbf: scala.collection.Factory[T, F[T]]) =
    Decoder.decodeCollect(decoder, limit)(buffer)
}
