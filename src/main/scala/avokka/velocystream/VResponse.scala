package avokka.velocystream

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
  }
}
