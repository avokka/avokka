package avokka

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
  */
final class ChunkX(val x: Long) extends AnyVal {

  /**
    * first chunk of message
    * @return
    */
  def first: Boolean = (x & 1L) == 1L

  /**
    * number of chunk or total if first
    * @return
    */
  def index: Long = x >> 1

  /**
    * if this chunk contains the whole message (no buffer needed)
    * @return
    */
  def single: Boolean = first && index == 1

  /**
    * the order of chunk in message
    * @return long order
    */
  def position: Long = if (first) 0 else index

  /**
    * next chunkx for a message
    */
  def next: ChunkX = ChunkX(first = false, position + 1)
}

object ChunkX {
  def apply(first: Boolean, index: Long): ChunkX = {
    val x = index << 1 | (if (first) 1L else 0L)
    new ChunkX(x)
  }

  /*
  def apply(index: Long, count: Long): ChunkX = {
    val first = index == 0
    apply(first, if (first) count else index)
  }
*/

  val codec: Codec[ChunkX] = uint32L.xmap(new ChunkX(_), _.x)
}
