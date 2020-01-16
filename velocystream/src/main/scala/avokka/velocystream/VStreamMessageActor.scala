package avokka.velocystream

import akka.actor._
import scala.collection.mutable
// import avokka.velocypack._
import cats.syntax.foldable._
import cats.instances.list._
import scodec.interop.cats.ByteVectorMonoidInstance

import scala.concurrent.duration._

class VStreamMessageActor(id: Long, replyTo: ActorRef) extends Actor with ActorLogging {
  import VStreamMessageActor._

  var expected: Option[Long] = None

  val stack: mutable.ListBuffer[VStreamChunk] = mutable.ListBuffer.empty

  val kill = context.system.scheduler.scheduleOnce(1.minute, self, PoisonPill)(context.dispatcher)

  override def postStop(): Unit = {
    stack.clear()
    if (!kill.isCancelled) {
      replyTo ! Status.Failure(new IllegalStateException(s"message #$id did not receive a response"))
    }
  }

  def sendMessageReply(message: VStreamMessage): Unit = {
    replyTo ! Status.Success(message)
    kill.cancel()
    context.stop(self)
  }

  override def receive: Actor.Receive = {

    case ChunkReceived(chunk) if chunk.x.isWhole =>
      // solo chunk, bypass stack computation
      sendMessageReply(VStreamMessage(id, chunk.data))

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
        sendMessageReply(VStreamMessage(id, bytes))
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
