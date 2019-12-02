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
  def isFirst: Boolean = VChunk.isFirstChunk(chunkX)
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

  @scala.annotation.tailrec
  def split(data: ByteVector, acc: Vector[ByteVector] = Vector.empty): Vector[ByteVector] = {
    if (data.size > maxLength) split(data.drop(maxLength), acc :+ data.take(maxLength))
    else acc :+ data
  }

  val maxLength: Long = 30000L

  def split(message: VMessage): Vector[VChunk] = {
    val s = split(message.data)
    val ln = s.length
    s.zipWithIndex.map { case (ch, idx) => apply(message, idx + 1, ln, ch) }
  }

  def isFirstChunk(chunkX: Long): Boolean = (chunkX & 1L) == 1

  implicit val codec: Codec[VChunk] = {
    ("length" | uint32L) >>:~ { length =>
      ("chunkX" | uint32L) ::
      ("messageId" | int64L) ::
      ("messageLength" | int64L) ::
      ("data" | bytes)
    }
  }.as[VChunk]
}