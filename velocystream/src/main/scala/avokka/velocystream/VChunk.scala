package avokka.velocystream

import scodec._
import scodec.bits._
import scodec.codecs._

case class VChunk
(
  length: Long,
  x: VChunk.X,
  messageId: Long,
  messageLength: Long,
  data: ByteVector
)
{
  // def isFirst: Boolean = x.first // (chunkX & 1L) == 1
  // def chunk: Long = x.number // chunkX >> 1
}

object VChunk
{
  case class X(first: Boolean, number: Long)
  object X {
    def apply(nr: Long, size: Long): X = X(nr == 1, if (nr == 1) size else nr)
  }

  def apply(message: VMessage, nr: Long, size: Long, data: ByteVector): VChunk = {
    VChunk(
      length = 4L + 4L + 8L + 8L + data.size,
      x = X(nr, size),
      messageId = message.id,
      messageLength = message.data.size,
      data = data
    )
  }

  val maxLength: Long = 30000L

  val xCodec: Codec[X] = uint32L.xmap(
    i => X(number = i >> 1, first = (i & 1L) == 1L),
    x => x.number << 1 | (if(x.first) 1L else 0L)
  )

  implicit val codec: Codec[VChunk] = {
    ("length" | uint32L) >>:~ { length =>
      ("chunkX" | xCodec) ::
      ("messageId" | int64L) ::
      ("messageLength" | int64L) ::
      ("data" | bytes)
    }
  }.as[VChunk]
}