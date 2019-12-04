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

class VClient(implicit materializer: ActorMaterializer) extends Actor {
  import VClient._
  import context.{dispatcher, system}

  val messageId = new AtomicLong()
  val promises = mutable.LongMap.empty[Promise[VResponse]]

  /*
  val VST = GraphDSL.create() { implicit builder =>
    val merge = builder.add(Concat[ByteString]())
    Source.single(ByteString(VST_HANDSHAKE)) ~> merge.in(0)
    FlowShape(merge.in(1), merge.out)
  }.named("VST")
*/

  val connection = Tcp().outgoingConnection("bak", 8529)

  val in = Flow[VMessage]
    .log("SEND message")
    .flatMapMerge(3, m => Source(m.chunks()))
    .log("SEND chunk")
    .map { chunk =>
      ByteString.fromArrayUnsafe(VChunk.codec.encode(chunk).require.toByteArray)
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
      VChunk.codec.decodeValue(BitVector(bs)).require
    }
    .log("RECV chunk")
    .via(new VChunkMessageStage)
    .log("RECV message")
    .map { m => VResponse.from(m.id, m.data.bits).require }
    .log("RECV response")

  val q = Source.queue[VMessage](100, OverflowStrategy.fail)

  val gr = q.via(in)
    //.groupBy(2, _.size > 30)
    .via(connection)
    //.mergeSubstreams
    .via(out)

  val conn = gr.to(Sink.actorRef(self, MEND)).run()

  override def receive: Receive = {
    case MEND => //context.stop(self)

    case b: ByteVector => {
      val message = VMessage(messageId.incrementAndGet(), b)
      val promise = Promise[VResponse]()
      promises.update(message.id, promise)
      conn.offer(message).map({
        case QueueOfferResult.Enqueued => promise
        case QueueOfferResult.Dropped => promise.failure(new RuntimeException("queue drop"))
        case QueueOfferResult.Failure(cause) => promise.failure(cause)
        case QueueOfferResult.QueueClosed => promise.failure(new RuntimeException("queue closed"))
      }).flatMap(_.future) pipeTo sender()
    }

    case r: VResponse => {
      promises.remove(r.messageId).foreach(_.success(r))
    }

    case _ =>
  }
}

object VClient {
  val VST_HANDSHAKE = "VST/1.1\r\n\r\n"

  case object MEND
}