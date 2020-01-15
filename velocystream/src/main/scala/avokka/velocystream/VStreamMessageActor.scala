package avokka.velocystream

import akka.actor._
import scala.collection.mutable
// import avokka.velocypack._
import cats.syntax.foldable._
import cats.instances.list._
import scodec.interop.cats.ByteVectorMonoidInstance

class VStreamMessageActor(id: Long, replyTo: ActorRef) extends Actor with ActorLogging {
  import VStreamMessageActor._

  var expected: Option[Long] = None

  val stack: mutable.ListBuffer[VStreamChunk] = mutable.ListBuffer.empty

  override def postStop(): Unit = {
    stack.clear()
  }

  override def receive: Actor.Receive = {

    case ChunkReceived(chunk) if chunk.x.isWhole =>
      // solo chunk, bypass stack computation
      val message = VStreamMessage(id, chunk.data)
      replyTo ! message
      context.stop(self)

    case ChunkReceived(chunk) =>
      // first chunk index is the total number of expected chunks
      if (chunk.x.first) {
        expected = Some(chunk.x.index)
      }
      // push chunk in stack
      stack += chunk
      // check completeness
      if (expected.contains(stack.length.toLong)) {
        // reorder bytes by chunk position
        val bytes = stack.result.sorted(chunkOrder).foldMap(_.data)
        val message = VStreamMessage(id, bytes)
        replyTo ! message
        context.stop(self)
      }

  }
}

object VStreamMessageActor {
  val chunkOrder: Ordering[VStreamChunk] = Ordering.by(_.x.position)

  case class ChunkReceived(
      chunk: VStreamChunk
  )

  def props(id: Long, replyTo: ActorRef): Props = Props(new VStreamMessageActor(id, replyTo))
}
