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

    def chunk(n: Long, slice: ByteVector): Stream[VChunk] = {
      val (head, tail) = slice.splitAt(length)
      VChunk(this, n, count, head) #:: (if (tail.isEmpty) Stream.Empty else chunk(n + 1, tail))
    }
    chunk(1, data)
  }
}

object VMessage {
  val messageId = new AtomicLong()

  def apply(data: ByteVector): VMessage = VMessage(
    id = messageId.incrementAndGet(),
    data
  )

}