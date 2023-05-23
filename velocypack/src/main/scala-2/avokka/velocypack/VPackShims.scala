package avokka.velocypack

import scodec.bits.BitVector
import scodec.{Attempt, DecodeResult, Decoder}

trait VPackShims {

  type Factory[-A, +C] = _root_.scala.collection.Factory[A, C]

  implicit final class DecoderOps[T](private val decoder: Decoder[T]) {

    def collect[F[_], A](buffer: BitVector, limit: Option[Int])(implicit cbf: Factory[T, F[T]]) = Decoder.decodeCollect(decoder, limit)(buffer)

  }
}