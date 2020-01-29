package avokka.velocystream

import akka.actor._

import scala.collection.mutable
import cats.syntax.foldable._
import cats.instances.list._
import scodec.interop.cats.ByteVectorMonoidInstance

import scala.concurrent.duration._

/** actor waiting for chunks from server and reply with complete message
  *
  * @param id message id
  * @param replyTo actor which sent the request
  */
class VStreamMessageActor(id: Long, replyTo: ActorRef) extends Actor with ActorLogging {
  import VStreamMessageActor._
  import context.dispatcher

  /** number of chunks expected to complete message */
  private var expected: Option[Long] = None

  /** stack of received chunks */
  private val buffer: mutable.ListBuffer[VStreamChunk] = mutable.ListBuffer.empty

  /** suicide after 1 minute without any complete message */
  private val kill = context.system.scheduler.scheduleOnce(1.minute, self, PoisonPill)
  private val replyFailure = Status.Failure(new IllegalStateException(s"message #$id did not receive a response"))

  override def postStop(): Unit = {
    // cleanup
    buffer.clear()
    // no reply sent, send a failure
    if (!kill.isCancelled) {
      replyTo ! replyFailure
    }
  }

  /** replies the complete message to the asker
    *
    * @param message complete message
    */
  def replyMessage(message: VStreamMessage): Unit = {
    replyTo ! Status.Success(message)
    kill.cancel()
    context.stop(self)
  }

  override def receive: Actor.Receive = {

    // solo chunk, bypass stack computation
    case VStreamReader.ChunkReceived(chunk) if chunk.x.single =>
      replyMessage(VStreamMessage(id, chunk.data))

    case VStreamReader.ChunkReceived(chunk) =>
      // first chunk index is the total number of expected chunks
      if (chunk.x.first) {
        expected = Some(chunk.x.index)
      }
      // push chunk in stack
      buffer += chunk
      // check completeness
      if (expected.contains(buffer.length.toLong)) {
        // reorder bytes by chunk position
        val bytes = buffer.result.sorted(chunkOrder).foldMap(_.data)
        replyMessage(VStreamMessage(id, bytes))
      }

  }
}

object VStreamMessageActor {
  val chunkOrder: Ordering[VStreamChunk] = Ordering.by(_.x.position)

  def props(id: Long, replyTo: ActorRef): Props = Props(new VStreamMessageActor(id, replyTo))
}
