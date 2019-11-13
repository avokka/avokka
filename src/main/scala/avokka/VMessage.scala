package avokka

import com.arangodb.velocypack.VPackSlice
import scodec.bits.ByteVector

case class VMessage
(
  messageId: Long,
  head: VPackSlice,
  body: Option[VPackSlice]
)

object VMessage {
  def apply(id: Long, bytes: ByteVector): VMessage = {
    val bs = bytes.toArray
    val head = new VPackSlice(bs)
    val headSize = head.getByteSize
    val body = if (bytes.size > headSize) Some(new VPackSlice(bs, headSize)) else None
    VMessage(id, head, body)
  }
}
