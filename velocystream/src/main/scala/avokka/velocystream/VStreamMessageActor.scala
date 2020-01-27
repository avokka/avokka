package avokka.velocystream

import akka.actor._

import scala.collection.mutable
import cats.syntax.foldable._
import cats.instances.list._
import scodec.interop.cats.ByteVectorMonoidInstance

import scala.concurrent.duration._

class VStreamMessageActor(id: Long, replyTo: ActorRef) extends Actor with ActorLogging {
  import VStreamMessageActor._
  import context.dispatcher

  private var expected: Option[Long] = None

  private val stack: mutable.ListBuffer[VStreamChunk] = mutable.ListBuffer.empty

  private val kill = context.system.scheduler.scheduleOnce(1.minute, self, PoisonPill)
  private val replyFailure = Status.Failure(new IllegalStateException(s"message #$id did not receive a response"))

  override def postStop(): Unit = {
    stack.clear()
    if (!kill.isCancelled) {
      replyTo ! replyFailure
    }
  }

  def sendMessageReply(message: VStreamMessage): Unit = {
    replyTo ! Status.Success(message)
    kill.cancel()
    context.stop(self)
  }

  override def receive: Actor.Receive = {

    case VStreamConnectionReader.ChunkReceived(chunk) if chunk.x.isWhole =>
      // solo chunk, bypass stack computation
      sendMessageReply(VStreamMessage(id, chunk.data))

    case VStreamConnectionReader.ChunkReceived(chunk) =>
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

  def props(id: Long, replyTo: ActorRef): Props = Props(new VStreamMessageActor(id, replyTo))
}
