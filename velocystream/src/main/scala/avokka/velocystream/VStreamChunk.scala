package avokka.velocystream

import cats.Show
import cats.syntax.show._
import scodec.bits.ByteVector
import scodec.interop.cats._

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

  /** split chunk data at length bytes
    *
    * @param length max number of bytes of data
    * @return chunk and optionally remainder
    */
  def split(length: Long): (VStreamChunk, Option[VStreamChunk]) = {
    val (chunk, tail) = data.splitAt(length)
    copy(data = chunk) -> (if (tail.nonEmpty) Some(VStreamChunk(header.next, tail)) else None)
  //  withRemainder(VStreamChunk(header.next, tail)).whenA(tail.nonEmpty).as(copy(data = chunk))
  }
}

object VStreamChunk extends VStreamChunkCodec {

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

  /*
  val codec: Codec[VStreamChunk] = uint32L.consume { l =>
    VStreamChunkHeader.codec :: fixedSizeBytes(l - dataOffset, bytes)
  } {
    case (_, bv) => bv.size + dataOffset
  }.as
   */

  implicit val show: Show[VStreamChunk] = { c =>
    show"chunk(id=${c.header.id},${c.header.x},length=${c.header.length}) ${c.data}"
  }
}