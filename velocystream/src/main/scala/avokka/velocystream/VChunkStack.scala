package avokka.velocystream

import cats.data.Chain

/**
 * Stacks received message chunks and checks for completeness
 *
 * @param messageId message unique identifier
 * @param chunks list of chunks
 * @param received number of chunks received so far
 * @param expected number of chunks expected for the complete message
 */
case class VChunkStack
(
  messageId: Long,
  chunks: Chain[VChunk] = Chain.empty,
  received: Long = 0,
  expected: Option[Long] = None
)
{
  /**
   * adds a chunk to the stack
   *
   * @param chunk the message chunk
   * @return the stack updated
   */
  def push(chunk: VChunk): VChunkStack = {
    require(messageId == chunk.messageId, "wrong message id in chunk stack")
    copy(
      chunks = chunks :+ chunk,
      received = received + 1,
      expected = if (chunk.x.first) Some(chunk.x.number) else expected
    )
  }

  /**
   * test if stack is complete
   *
   * @return the message
   */
  def complete: Option[VMessage] = {
    if (expected.contains(received) && chunks.length == received) {
      // order chunks by position and reduce their data blobs
      val bytes = chunks.toVector.sortBy(_.x.position).map(_.data).reduce(_ ++ _)
      Some(VMessage(messageId, bytes))
    } else None
  }
}
