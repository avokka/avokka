package avokka.velocystream

import java.nio.ByteOrder

import akka.NotUsed
import akka.actor._
import akka.pattern.pipe
import akka.stream.scaladsl._
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import akka.util.ByteString
import scodec.bits.{BitVector, ByteVector}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

/** Velocystream client actor
  *
  */
class VStreamClient(conf: VStreamConfiguration, begin: Iterable[VStreamMessage])(implicit materializer: Materializer)
    extends Actor
    with ActorLogging {
  import VStreamClient._
  import context.{dispatcher, system}

  val promises = mutable.LongMap.empty[Promise[VStreamMessage]]
  // val senders = mutable.LongMap.empty[ActorRef]

  val connection = Tcp().outgoingConnection(conf.host, conf.port)

  val in = Flow[VStreamMessage]
    .prepend(Source.fromIterator(() => begin.iterator))
    .log("SEND message")
    .flatMapMerge(3, m => Source(m.chunks(conf.chunkLength)))
    .log("SEND chunk")
    .map { chunk =>
      ByteString.fromArrayUnsafe(VStreamChunk.codec.encode(chunk).require.toByteArray)
    }
    .prepend(Source.single(ByteString(VST_HANDSHAKE)))

  val out = Flow[ByteString]
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

  val protocol: Flow[VStreamMessage, VStreamMessage, NotUsed] = in.via(connection).via(out)

  val q = Source.queue[VStreamMessage](conf.queueSize, OverflowStrategy.fail)

  val gr = q.via(protocol)
  //    .via(in)
    //.groupBy(2, _.size > 30)
//    .via(connection)
    //.mergeSubstreams
//    .via(out)

  val conn = gr.map(MessageReceived).to(Sink.actorRef(self, ConnectionTerminated)).run()

  override def receive: Receive = {
    case ConnectionTerminated => //context.stop(self)

    case m: MessageSend => {
      val message = m.message
      val promise = Promise[VStreamMessage]()
      promises.update(message.id, promise)
      conn
        .offer(message)
        .map({
          case QueueOfferResult.Enqueued       => promise
          case QueueOfferResult.Dropped        => promise.failure(new RuntimeException("queue drop"))
          case QueueOfferResult.Failure(cause) => promise.failure(cause)
          case QueueOfferResult.QueueClosed    => promise.failure(new RuntimeException("queue closed"))
        })
        .flatMap(_.future) pipeTo sender()
    }

    case m: MessageReceived => {
      promises.remove(m.message.id).foreach(_.success(m.message))
    }

    case _ =>
  }
}

object VStreamClient {

  def apply(conf: VStreamConfiguration, begin: Iterable[VStreamMessage])(implicit materializer: Materializer): Props =
    Props(new VStreamClient(conf, begin)(materializer))

  val VST_HANDSHAKE = "VST/1.1\r\n\r\n"

  case class MessageSend(message: VStreamMessage)
  case class MessageReceived(message: VStreamMessage)
  case object ConnectionTerminated
}
