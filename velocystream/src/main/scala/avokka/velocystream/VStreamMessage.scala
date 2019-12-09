package avokka.velocystream

import scodec.bits.ByteVector

case class VStreamMessage
(
  id: Long,
  data: ByteVector
) {

  def chunks(length: Long = VStreamChunk.maxLength): Stream[VStreamChunk] =  {
    val count = (data.size.toDouble / length).ceil.toLong

    def stream(n: Long, slice: ByteVector): Stream[VStreamChunk] = {
      if (slice.isEmpty) Stream.Empty
      else {
        VStreamChunk(this, n, count, slice.take(length)) #:: stream(n + 1, slice.drop(length))
      }
    }
    stream(0, data)
  }
}
