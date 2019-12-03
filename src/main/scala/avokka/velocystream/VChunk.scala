package avokka.velocystream

import scodec._
import scodec.bits._
import scodec.codecs._

case class VChunk
(
  length: Long,
  chunkX: Long,
  messageId: Long,
  messageLength: Long,
  data: ByteVector
)
{
  def isFirst: Boolean = (chunkX & 1L) == 1
  def chunk: Long = chunkX >> 1
}

object VChunk
{
  def apply(message: VMessage, nr: Long, size: Long, data: ByteVector): VChunk = {
    val chunkX = if (nr == 1) size << 1 | 1L else nr << 1
    VChunk(
      length = 4L + 4L + 8L + 8L + data.size,
      chunkX = chunkX,
      messageId = message.id,
      messageLength = message.data.size,
      data = data
    )
  }

  val maxLength: Long = 10 //30000L

  implicit val codec: Codec[VChunk] = {
    ("length" | uint32L) >>:~ { length =>
      ("chunkX" | uint32L) ::
      ("messageId" | int64L) ::
      ("messageLength" | int64L) ::
      ("data" | bytes)
    }
  }.as[VChunk]
}