package avokka

import scodec.Codec
import scodec.codecs.int64L

final case class ChunkHeader
(
  x: ChunkX,
  message: Long,
  messageLength: Long,
) {
  def next: ChunkHeader = copy(x = x.next)
}

object ChunkHeader {
  implicit val codec: Codec[ChunkHeader] = (ChunkX.codec :: int64L :: int64L).as[ChunkHeader]
}
