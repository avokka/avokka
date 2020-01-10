package avokka.velocystream

import java.net.InetSocketAddress
import java.nio.ByteOrder

import akka.NotUsed
import akka.actor._
import akka.stream.scaladsl._
import akka.util.ByteString
import scodec.bits.BitVector

import scala.concurrent.duration._

/** Velocystream flow
  *
  */
class VStreamFlow(conf: VStreamConfiguration, begin: Source[VStreamMessage, _])(implicit system: ActorSystem) {
  import VStreamFlow._

  private val address = InetSocketAddress.createUnresolved(conf.host, conf.port)
  private val connection = Tcp().outgoingConnection(
    remoteAddress = address,
    connectTimeout = 5.seconds,
    idleTimeout = 1.hour
  )

  private val in = Flow[VStreamMessage]
    .prepend(begin)
    .log("SEND message")
    .flatMapMerge(3, m => Source(m.chunks(conf.chunkLength)))
    .log("SEND chunk")
    .map { chunk =>
      VStreamChunk.codec.encode(chunk).require
    }
    .map { bits =>
      ByteString.fromArrayUnsafe(bits.toByteArray)
    }
    .prepend(Source.single(VST_HANDSHAKE))

  private val out = Flow[ByteString]
    .via(
      Framing.lengthField(
        fieldLength = 4,
        fieldOffset = 0,
        maximumFrameLength = Int.MaxValue,
        byteOrder = ByteOrder.LITTLE_ENDIAN,
        computeFrameSize = (_, l) => l
      ))
    .map { bs =>
      VStreamChunk.codec.decodeValue(BitVector(bs)).require
    }
    .log("RECV chunk")
    .via(new VStreamChunkMessageStage)
    .log("RECV message")

  val protocol: Flow[VStreamMessage, VStreamMessage, NotUsed] = {
    RestartFlow.withBackoff(
      minBackoff = 1.second,
      maxBackoff = 30.seconds,
      randomFactor = 0.2,
      maxRestarts = -1)(() => in.via(connection).via(out))
  }
}

object VStreamFlow {
  val VST_HANDSHAKE = ByteString("VST/1.1\r\n\r\n")
}
