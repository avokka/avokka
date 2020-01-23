import avokka.velocystream.{VStreamChunk, VStreamMessage}
import scodec.bits._

import scala.collection.mutable

implicit val chunkOrdering: Ordering[VStreamChunk] = Ordering.by[VStreamChunk, Long](chunk => chunk.messageId + chunk.x.position).reverse

val sendQueue: mutable.PriorityQueue[VStreamChunk] = mutable.PriorityQueue.empty

VStreamMessage(2, hex"00").chunks(1).foreach {
  sendQueue += _
}

VStreamMessage(1, hex"aabbcc").chunks(1).foreach {
  sendQueue += _
}

sendQueue.dequeueAll.foreach(println(_))

println(sendQueue)
