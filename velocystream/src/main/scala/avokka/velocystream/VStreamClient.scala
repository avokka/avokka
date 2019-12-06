package avokka.velocystream

import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong

import akka.actor._
import akka.pattern.pipe
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.util.ByteString
import scodec.bits.{BitVector, ByteVector}

import scala.collection.mutable
import scala.concurrent.Promise

class VStreamClient(host: String, port: Int)(implicit materializer: ActorMaterializer) extends Actor with ActorLogging {
  import VStreamClient._
  import context.{dispatcher, system}

  val messageId = new AtomicLong()
  val promises = mutable.LongMap.empty[Promise[VStreamMessage]]

  /*
  val VST = GraphDSL.create() { implicit builder =>
    val merge = builder.add(Concat[ByteString]())
    Source.single(ByteString(VST_HANDSHAKE)) ~> merge.in(0)
    FlowShape(merge.in(1), merge.out)
  }.named("VST")
*/

  val connection = Tcp().outgoingConnection(host, port)

  val in = Flow[VStreamMessage]
    .log("SEND message")
    .flatMapMerge(3, m => Source(m.chunks()))
    .log("SEND chunk")
    .map { chunk =>
      ByteString.fromArrayUnsafe(VStreamChunk.codec.encode(chunk).require.toByteArray)
    }
    .prepend(Source.single(ByteString(VST_HANDSHAKE)))

  val out = Flow[ByteString]
    .via(Framing.lengthField(
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

  val q = Source.queue[VStreamMessage](100, OverflowStrategy.fail)

  val gr = q.via(in)
    //.groupBy(2, _.size > 30)
    .via(connection)
    //.mergeSubstreams
    .via(out)

  val conn = gr.to(Sink.actorRef(self, MEND)).run()

  override def receive: Receive = {
    case MEND => //context.stop(self)

    case b: ByteVector => {
      val message = VStreamMessage(messageId.incrementAndGet(), b)
      val promise = Promise[VStreamMessage]()
      promises.update(message.id, promise)
      conn.offer(message).map({
        case QueueOfferResult.Enqueued => promise
        case QueueOfferResult.Dropped => promise.failure(new RuntimeException("queue drop"))
        case QueueOfferResult.Failure(cause) => promise.failure(cause)
        case QueueOfferResult.QueueClosed => promise.failure(new RuntimeException("queue closed"))
      }).flatMap(_.future) pipeTo sender()
    }

    case r: VStreamMessage => {
      promises.remove(r.id).foreach(_.success(r))
    }

    case _ =>
  }
}

object VStreamClient {
  val VST_HANDSHAKE = "VST/1.1\r\n\r\n"

  case object MEND
}