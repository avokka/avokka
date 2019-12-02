package avokka.velocystream

import java.util.concurrent.atomic.AtomicLong

import scodec.bits.ByteVector

case class VMessage
(
  id: Long,
  data: ByteVector
) {
  def chunks: Vector[VChunk] = VChunk.split(this)
}

object VMessage {
  val messageId = new AtomicLong()

  def apply(data: ByteVector): VMessage = VMessage(
    id = messageId.incrementAndGet(),
    data
  )
}