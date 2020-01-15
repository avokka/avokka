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
case class VStreamChunk(
    length: Long,
    x: VStreamChunkX,
    messageId: Long,
    messageLength: Long,
    data: ByteVector
)

object VStreamChunk {
  def apply(message: VStreamMessage, index: Long, count: Long, data: ByteVector): VStreamChunk = {
    VStreamChunk(
      length = 4L + 4L + 8L + 8L + data.size,
      x = VStreamChunkX(index, count),
      messageId = message.id,
      messageLength = message.data.size,
      data = data
    )
  }

  implicit val codec: Codec[VStreamChunk] = {
    ("length" | uint32L) >>:~ { length =>
      ("chunkX" | VStreamChunkX.codec) ::
        ("messageId" | int64L) ::
        ("messageLength" | int64L) ::
        ("data" | fixedSizeBytes(length - 4 - 4 - 8 - 8, bytes))
    }
  }.as[VStreamChunk]
}
