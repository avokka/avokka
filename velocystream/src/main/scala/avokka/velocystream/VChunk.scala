package avokka.velocystream

import scodec._
import scodec.bits._
import scodec.codecs._

/**
 * Message chunk
 *
 * @param length total length in bytes of the current chunk, including the header
 * @param x chunk/isFirstChunk
 * @param messageId a unique identifier (zero is reserved for not set ID)
 * @param messageLength the total size of the message
 * @param data blob of chunk
 */
case class VChunk
(
  length: Long,
  x: VChunkX,
  messageId: Long,
  messageLength: Long,
  data: ByteVector
)

object VChunk
{
  def apply(message: VMessage, number: Long, total: Long, data: ByteVector): VChunk = {
    VChunk(
      length = 4L + 4L + 8L + 8L + data.size,
      x = VChunkX(number, total),
      messageId = message.id,
      messageLength = message.data.size,
      data = data
    )
  }

  val maxLength: Long = 30000L

  implicit val codec: Codec[VChunk] = {
    ("length" | uint32L) >>:~ { length =>
      ("chunkX" | VChunkX.codec) ::
      ("messageId" | int64L) ::
      ("messageLength" | int64L) ::
      ("data" | bytes)
    }
  }.as[VChunk]
}