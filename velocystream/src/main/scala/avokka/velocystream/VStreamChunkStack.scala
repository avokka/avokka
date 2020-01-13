package avokka.velocystream

import cats.data.Chain
import cats.syntax.foldable._
import cats.instances.vector._
import scodec.interop.cats.ByteVectorMonoidInstance

/**
  * Stacks received message chunks and checks for completeness
  *
  * @param messageId message unique identifier
  * @param chunks list of chunks
  * @param received number of chunks received so far
  * @param expected number of chunks expected for the complete message
  */
case class VStreamChunkStack(
    messageId: Long,
    chunks: Chain[VStreamChunk] = Chain.empty,
    received: Long = 0,
    expected: Option[Long] = None
) {

  /**
    * adds a chunk to the stack
    *
    * @param chunk the message chunk
    * @return the stack updated
    */
  def push(chunk: VStreamChunk): VStreamChunkStack = {
    require(messageId == chunk.messageId, "wrong message id in chunk stack")
    copy(
      // append chunk
      chunks = chunks :+ chunk,
      // increment received counter
      received = received + 1,
      // first chunk index is the total number of expected chunks
      expected = if (chunk.x.first) Some(chunk.x.index) else expected
    )
  }

  /**
    * test if stack is complete
    *
    * @return the message
    */
  def complete: Option[VStreamMessage] = {
    if (expected.contains(received) && chunks.length == received) {
      // order chunks by position and reduce their data blobs
      val bytes = chunks.toVector.sortBy(_.x.position).foldMap(_.data) //.reduce(_ ++ _)
      Some(VStreamMessage(messageId, bytes))
    } else None
  }
}
