package avokka.velocypack

import scodec.bits.BitVector
import scodec.*

trait DecoderOpsShims[T] {
  def decoder: Decoder[T]

  def collect[F[_], A](buffer: BitVector, limit: Option[Int])(
      implicit cbf: scala.collection.Factory[T, F[T]]): Attempt[DecodeResult[F[T]]] =
    Decoder.decodeCollect(decoder, limit)(buffer)
}
