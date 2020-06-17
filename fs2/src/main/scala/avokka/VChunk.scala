package avokka

import cats.Applicative
import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs.{bytes, fixedSizeBytes, uint32L}
import scodec.stream.{StreamDecoder, StreamEncoder}
import shapeless.{::, HNil}
import cats.syntax.applicative._
import cats.syntax.functor._

final case class VChunk
(
  header: VChunkHeader,
  data: ByteVector
) {

  /** split chunk data at length bytes, returns the chunk
    *
    * @param length max number of bytes of data
    * @param withRemainder what to do with data tail
    * @tparam F applicative context
    * @return
    */
  def take[F[_]: Applicative](length: Long, withRemainder: VChunk => F[Unit]): F[VChunk] = {
    val (chunk, tail) = data.splitAt(length)
    withRemainder(VChunk(header.next, tail)).whenA(tail.nonEmpty).as(copy(data = chunk))
  }
}

object VChunk {

  // 4 chunk length + 4 chunkx + 8 message id + 8 message length
  val headerOffset: Long = 24

  val codec: Codec[VChunk] = uint32L.consume { l =>
    VChunkHeader.codec :: fixedSizeBytes(l - headerOffset, bytes)
  } {
    case _ :: bv :: HNil => bv.size + headerOffset
  }.as

  val streamDecoder: StreamDecoder[VChunk] = StreamDecoder.many(codec)
  val streamEncoder: StreamEncoder[VChunk] = StreamEncoder.many(codec)
}