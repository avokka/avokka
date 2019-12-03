package avokka.velocystream

import cats.data.Chain

case class VChunkStack
(
  messageId: Long,
  chunks: Chain[VChunk] = Chain.empty,
  received: Long = 0,
  expected: Option[Long] = None
)
{
  def push(chunk: VChunk): VChunkStack = {
    require(messageId == chunk.messageId, "wrong message id in chunk stack")
    copy(
      chunks = chunks :+ chunk,
      received = received + 1,
      expected = if (chunk.x.first) Some(chunk.x.number) else expected
    )
  }

  def complete: Option[VMessage] = {
    if (expected.contains(received) && chunks.length == received) {
      val bytes = chunks.toVector.sortBy(_.x.position).map(_.data).reduce(_ ++ _)
      Some(VMessage(messageId, bytes))
    } else None
  }
}
