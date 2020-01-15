package avokka.velocystream

import java.util.concurrent.atomic.AtomicLong

import scodec.bits.ByteVector

/**
  * velocystream message in or out
  * @param id identifier
  * @param data data bytes
  */
case class VStreamMessage(
    id: Long,
    data: ByteVector
) {

  /**
    * splits the message in a stream of chunks
    * @param length maximum length of data chunk
    * @return stream of chunks
    */
  def chunks(length: Long = VStreamConfiguration.CHUNK_LENGTH_DEFAULT): Stream[VStreamChunk] = {
    val count = (data.size.toDouble / length).ceil.toLong

    def stream(n: Long, slice: ByteVector): Stream[VStreamChunk] = {
      if (slice.isEmpty) Stream.Empty
      else VStreamChunk(this, n, count, slice.take(length)) #:: stream(n + 1, slice.drop(length))
    }

    stream(0, data)
  }
}

object VStreamMessage {
  val id = new AtomicLong()
  def create(data: ByteVector): VStreamMessage = VStreamMessage(id.incrementAndGet(), data)
}