package avokka.velocypack.codecs

import cats.data.Chain
import scodec.{Attempt, DecodeResult, Decoder}
import scodec.bits.BitVector

import scala.annotation.tailrec

private[codecs] class ChainDecoder[T](decoder: Decoder[T], limit: Option[Long] = None) extends Decoder[Chain[T]] {

  @tailrec
  private def loop(bits: BitVector, iter: Option[Long], acc: Chain[T] = Chain.empty): Attempt[DecodeResult[Chain[T]]] = {
    if (bits.isEmpty) Attempt.successful(DecodeResult(acc, bits))
    else if (iter.contains(0L)) Attempt.successful(DecodeResult(acc, bits))
    else decoder.decode(bits) match {
      case Attempt.Successful(value) => loop(value.remainder, iter.map(_ - 1), acc.append(value.value))
      case f : Attempt.Failure => f
    }
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[Chain[T]]] = loop(bits, limit)
}
