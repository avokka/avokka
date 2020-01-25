package avokka.velocystream

import akka.actor._
import akka.io.Tcp
import avokka.velocystream.VStreamConnection.ChunkReceived
import scodec.{Attempt, Codec, Err}
import scodec.bits.BitVector

import scala.collection.mutable

class VStreamConnectionReader() extends Actor with ActorLogging {
  import VStreamConnectionReader._

  val recvBuffer: mutable.ListBuffer[BitVector] = mutable.ListBuffer.empty

  override def receive: Receive = {
    case MessageInit(id) =>
      context.actorOf(VStreamMessageActor.props(id, sender()), s"message-${id}")

    case Tcp.Received(data) =>
      val connection = sender()
      log.debug("received data {} bytes", data.length)
      recvBuffer += BitVector(data.asByteBuffer)

      val theBuffer = recvBuffer.result().reduce(_ ++ _)
      Codec.decodeCollect(VStreamChunk.codec, None)(theBuffer) match {
        case Attempt.Successful(result) => {
          val chunks = result.value
          log.debug("successful decode {}", chunks.map(_.messageId))
          chunks.foreach { chunk =>
            context.child(s"message-${chunk.messageId}").foreach { child =>
              log.debug("send chunk to child {}", child)
              child ! ChunkReceived(chunk)
            }
          }
          recvBuffer.clear()
          if (result.remainder.nonEmpty) {
            recvBuffer += result.remainder
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
}
