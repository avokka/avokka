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
  def isFirst: Boolean = (chunkX & 0x1) == 1
  def chunk: Long = chunkX >> 1
}

object VChunk
{
  implicit val codec: Codec[VChunk] = {
    ("length" | uint32L) ::
    ("chunkX" | uint32L) ::
    ("messageId" | int64L) ::
    ("messageLength" | int64L) ::
    ("data" | bytes)
  }.as[VChunk]
}