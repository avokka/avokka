package avokka.velocystream

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.functor._
import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs.{bytes, fixedSizeBytes, uint32L}
import shapeless.{::, HNil}

/** chunk of message
  *
  * @param header header to reassemble message
  * @param data chunk payload
  */
final case class VStreamChunk
(
  header: VStreamChunkHeader,
  data: ByteVector
) {

  /** split chunk data at length bytes, do something with remainder and returns the chunk
    *
    * @param length max number of bytes of data
    * @param withRemainder what to do with data tail
    * @tparam F applicative context
    * @return
    */
  def take[F[_]: Applicative](length: Long, withRemainder: VStreamChunk => F[Unit]): F[VStreamChunk] = {
    val (chunk, tail) = data.splitAt(length)
    withRemainder(VStreamChunk(header.next, tail)).whenA(tail.nonEmpty).as(copy(data = chunk))
  }
}

object VStreamChunk {

  def apply(message: VStreamMessage, index: Long, count: Long, data: ByteVector): VStreamChunk = {
    VStreamChunk(
      header = VStreamChunkHeader(
        x = VStreamChunkX(index, count),
        id = message.id,
        length = message.data.size,
      ),
      data = data
    )
  }

  // 4 chunk length + 4 chunkx + 8 message id + 8 message length
  val dataOffset: Long = 24

  val codec: Codec[VStreamChunk] = uint32L.consume { l =>
    VStreamChunkHeader.codec :: fixedSizeBytes(l - dataOffset, bytes)
  } {
    case _ :: bv :: HNil => bv.size + dataOffset
  }.as

}