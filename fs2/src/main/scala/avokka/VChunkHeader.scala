package avokka

import scodec.Codec
import scodec.codecs.int64L

final case class VChunkHeader
(
  x: VChunkX,
  message: Long,
  messageLength: Long,
) {
  def next: VChunkHeader = copy(x = x.next)
}

object VChunkHeader {
  implicit val codec: Codec[VChunkHeader] = (VChunkX.codec :: int64L :: int64L).as[VChunkHeader]
}
