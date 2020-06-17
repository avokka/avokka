package avokka

import scodec.Codec
import scodec.codecs.int64L

/** header of chunk
  *
  * @param x chunkx
  * @param id unique message identifier
  * @param length total size of message in bytes
  */
final case class VChunkHeader
(
  x: VChunkX,
  id: Long,
  length: Long,
) {
  /** @return header of the next chunk for the same message */
  def next: VChunkHeader = copy(x = x.next)
}

object VChunkHeader {
  implicit val codec: Codec[VChunkHeader] = (VChunkX.codec :: int64L :: int64L).as[VChunkHeader]
}
