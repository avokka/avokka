package avokka.velocystream

import akka.actor._
import akka.io.Tcp
import scodec.{Attempt, Codec, Err}
import scodec.bits.BitVector

import scala.collection.mutable

/**
  * velocystream connection reader handles Tcp.Received events,
  * accumulates bitvector and tries to decodes chunks,
  * then send those chunks to message actor children
  */
class VStreamConnectionReader() extends Actor with ActorLogging {
  import VStreamConnectionReader._

  val buffer: mutable.ListBuffer[BitVector] = mutable.ListBuffer.empty
//  val buffer: ByteStringBuilder = new ByteStringBuilder

  private def messageName(id: Long) = s"message-$id"

  override def receive: Receive = {
    case MessageInit(id) =>
      context.actorOf(VStreamMessageActor.props(id, sender()), messageName(id))

    case Tcp.Received(data) =>
      val connection = sender()
      log.debug("received data {} bytes", data.length)
      // buffer.append(data)
      buffer += BitVector(data.asByteBuffer)

      val bits = BitVector.concat(buffer.result()) //.reduce(_ ++ _)
      // val bits = BitVector(buffer.result())
      Codec.decodeCollect(VStreamChunk.codec, None)(bits) match {
        case Attempt.Successful(result) => {
          val chunks = result.value
          log.debug("successful decode {}", chunks.map(_.messageId))
          chunks.foreach { chunk =>
            context.child(messageName(chunk.messageId)).foreach { child =>
              log.debug("send chunk to child {}", child)
              child ! ChunkReceived(chunk)
            }
          }
          buffer.clear()
          if (result.remainder.nonEmpty) {
            buffer += result.remainder
           // buffer.append(ByteString(result.remainder.toByteBuffer))
          }
          connection ! Tcp.ResumeReading
        }
        case Attempt.Failure(cause: Err.InsufficientBits) =>
          log.debug("insufficent bits needed={} have={}", cause.needed, cause.have)
          connection ! Tcp.ResumeReading
        case Attempt.Failure(cause) =>
          log.error(cause.toString())
          connection ! Tcp.Close
      }

  }
}

object VStreamConnectionReader {
  def props(): Props = Props(new VStreamConnectionReader())

  case class MessageInit(id: Long)
  case class ChunkReceived(chunk: VStreamChunk)

}
