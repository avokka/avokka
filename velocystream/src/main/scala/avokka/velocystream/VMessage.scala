package avokka.velocystream

import java.util.concurrent.atomic.AtomicLong

import scodec.bits.ByteVector

case class VMessage
(
  id: Long,
  data: ByteVector
) {

  def chunks(length: Long = VChunk.maxLength): Stream[VChunk] =  {
    val count = (data.size.toDouble / length).ceil.toLong

    def stream(n: Long, slice: ByteVector): Stream[VChunk] = {
      if (slice.isEmpty) Stream.Empty
      else {
        VChunk(this, n, count, slice.take(length)) #:: stream(n + 1, slice.drop(length))
      }
    }
    stream(1, data)
  }
}

object VMessage {
  val messageId = new AtomicLong()

  def apply(data: ByteVector): VMessage = VMessage(
    id = messageId.incrementAndGet(),
    data
  )

}