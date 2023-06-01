package avokka.velocystream

import akka.actor._
import akka.io.Tcp
import akka.util.ByteString
import scodec.{Attempt, Codec, Err}
import scodec.bits.BitVector
import avokka.velocypack._

/**
  * velocystream connection reader handles Tcp.Received events,
  * accumulates bitvector and tries to decodes chunks,
  * then send those chunks to message actor children
  */
class VStreamReader() extends Actor with ActorLogging {
  import VStreamReader._

  /** buffer of received bytes */
  var buffer: ByteString = ByteString.empty

  /** the actor name of message decoder
    *
    * @param id message ID
    * @return actor name
    */
  private def messageName(id: Long) = s"message-$id"

  override def receive: Receive = {
    case MessageInit(id) =>
      // spawn an actor waiting to decode the response
      context.actorOf(VStreamMessageActor.props(id, sender()), messageName(id))

    case Tcp.Received(data) =>
      val connection = sender()
      log.debug("received data {} bytes", data.length)
      buffer ++= data

      // try to decode incoming bytes as chunks
      val bits = BitVector.view(buffer.asByteBuffer)
      VStreamChunk.codec.collect(bits, None) match {

        // success decode
        case Attempt.Successful(result) => {
          val chunks = result.value
          log.debug("successful decode {}", chunks.map(_.header.id))
          chunks.foreach { chunk =>
            // log.debug("decoded {}: {}", chunk.messageId, chunk.data.bits.asVPack.map(r => Show[VPack].show(r)))
            // send each chunk to corresponding message decoder actor
            context.child(messageName(chunk.header.id)).foreach { child =>
              log.debug("send chunk to child {}", child)
              child ! ChunkReceived(chunk)
            }
          }

          // reset buffer to remaining bytes of decoder
          buffer = ByteString(result.remainder.toByteBuffer) // ByteString.empty //.clear()
          /*
          if (result.remainder.nonEmpty) {
            buffer ++= result.remainder.toByteArray
          }
           */

          // pull more bytes
          connection ! Tcp.ResumeReading
        }

        // failure because of insufficents bits, keep reading
        case Attempt.Failure(cause: Err.InsufficientBits) =>
          log.debug("insufficent bits needed={} have={}", cause.needed, cause.have)
          connection ! Tcp.ResumeReading

        // failure in protocol, stop connection
        case Attempt.Failure(cause) =>
          log.error(cause.toString())
          connection ! Tcp.Close
      }

  }
}

object VStreamReader {
  def props(): Props = Props(new VStreamReader())

  final case class MessageInit(id: Long)
//  case object MessageReplied
  final case class ChunkReceived(chunk: VStreamChunk)

}
