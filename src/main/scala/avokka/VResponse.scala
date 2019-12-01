package avokka

import scodec.Attempt
import scodec.bits.BitVector

case class VResponse
(
  messageId: Long,
  header: VResponseHeader,
  body: BitVector
)

object VResponse {
  def from(id: Long, bits: BitVector): Attempt[VResponse] = {
    for {
      header <- VResponseHeader.codec.decode(bits)
    } yield VResponse(id, header.value, header.remainder)
/*
    val bs = bytes.toArray
    val head = new VPackSlice(bs)
    val headSize = head.getByteSize
    val body = if (bytes.size > headSize) Some(new VPackSlice(bs, headSize)) else None
    VResponse(id, head, body)
 */
  }
}
