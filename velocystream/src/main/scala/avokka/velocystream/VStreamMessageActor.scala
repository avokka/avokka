package avokka.velocystream

import akka.actor._
import akka.pattern.pipe
import avokka.velocypack._

import scala.concurrent.Promise

class VStreamMessageActor(id: Long, replyTo: ActorRef) extends Actor with ActorLogging {
  import VStreamMessageActor._
  import context.dispatcher

  var stack = VStreamChunkStack(id)

  override def receive: Actor.Receive = {

    case ChunkReceived(chunk) => {
      log.debug("chunk received {}", chunk.toString)
      if (chunk.x.isWhole) {
        // solo chunk, bypass stack merge computation
        val message = VStreamMessage(chunk.messageId, chunk.data)
        log.debug("message received {}", message.data.bits.asVPack)
        replyTo ! message
        context.stop(self)
//        pushMessage(message)
      } else {
        // push chunk in stack
        stack = stack.push(chunk)
        // check completeness
        stack.complete match {
          case Some(message) => {
            // a full message, remove stack from map
            replyTo ! message
            context.stop(self)
          }
          case None => {
            // stack is pending more chunks
          }
        }
      }
    }
  }
}

object VStreamMessageActor {
  case class ChunkReceived(
      chunk: VStreamChunk
  )

  def props(id: Long, replyTo: ActorRef): Props = Props(new VStreamMessageActor(id, replyTo))
}
