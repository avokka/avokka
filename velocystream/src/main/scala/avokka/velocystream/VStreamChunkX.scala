package avokka.velocystream

import scodec.Codec
import scodec.codecs.uint32L

/**
 * ChunkX chunk/isFirstChunk (upper 31bits/lowest bit)
 *
 * "chunk" and "isFirstChunk" are combined into an unsigned 32bit value
 *
 * chunk = chunkX >> 1
 * isFirstChunk = chunkX & 0x1
 *
 * For the first chunk of a message, the low bit of the second uint32_t is set, for all subsequent ones it is reset.
 * In the first chunk of a message, the number "chunk" is the total number of chunks in the message,
 * in all subsequent chunks, the number "chunk" is the current number of this chunk.
 *
 * @param first first chunk of message
 * @param index number of chunk or total if first
 */
case class VStreamChunkX
(
  first: Boolean,
  index: Long,
)
{
  /**
   * if this chunk contains the whole message (no buffer needed)
   * @return
   */
  def isWhole: Boolean = first && index == 1

  /**
   * the order of chunk in message
   * @return long order
   */
  def position: Long = if (first) 0 else index
}

object VStreamChunkX {

  def apply(index: Long, count: Long): VStreamChunkX = {
    val first = index == 0
    VStreamChunkX(first, if (first) count else index)
  }

  implicit val codec: Codec[VStreamChunkX] = uint32L.xmap(
    i => VStreamChunkX(index = i >> 1, first = (i & 1L) == 1L),
    x => x.index << 1 | (if(x.first) 1L else 0L)
  )
}