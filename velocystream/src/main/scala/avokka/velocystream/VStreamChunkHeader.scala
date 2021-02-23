package avokka.velocystream

import scodec.Codec
import scodec.codecs.int64L

/** header of chunk
  *
  * @param x chunkx
  * @param id unique message identifier
  * @param length total size of message in bytes
  */
final case class VStreamChunkHeader
(
  x: VStreamChunkX,
  id: Long,
  length: Long,
) {
  /** @return header of the next chunk for the same message */
  def next: VStreamChunkHeader = copy(x = x.next)
}

object VStreamChunkHeader {
  implicit val codec: Codec[VStreamChunkHeader] = (VStreamChunkX.codec :: int64L :: int64L).as
}
