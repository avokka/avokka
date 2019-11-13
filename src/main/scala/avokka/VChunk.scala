package avokka

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
  def isFirst: Boolean = VChunk.isFirstChunk(chunkX)
  def chunk: Long = chunkX >> 1
}

object VChunk
{
  def apply(messageId: Long, data: ByteVector): VChunk = {
    VChunk(
      length = data.size + 8 + 8 + 4 + 4,
      chunkX = 3,
      messageId = messageId,
      messageLength = data.size,
      data = data
    )
  }

  def isFirstChunk(chunkX: Long): Boolean = (chunkX & 0x1) == 1

  implicit val codec: Codec[VChunk] = {
    ("length" | uint32L) >>:~ { length =>
      ("chunkX" | uint32L) ::
      ("messageId" | int64L) ::
      ("messageLength" | int64L) ::
      ("data" | bytes)
    }
  }.as[VChunk]
}