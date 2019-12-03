package avokka.velocystream

import java.nio.ByteOrder

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy, QueueOfferResult}
import akka.util.ByteString
import scodec.bits.{BitVector, ByteVector}
import GraphDSL.Implicits._
import akka.pattern.pipe

import scala.concurrent.{Future, Promise}
import scala.collection.mutable

class VClient(implicit materializer: ActorMaterializer) extends Actor {
  import VClient._
  import context.system
  import context.dispatcher

  val promises = mutable.LongMap.empty[Promise[VResponse]]

  val VST = GraphDSL.create() { implicit builder =>
    val merge = builder.add(Concat[ByteString]())
    Source.single(ByteString("VST/1.1\r\n\r\n")) ~> merge.in(0)
    FlowShape(merge.in(1), merge.out)
  }.named("VST")

  val connection = Tcp().outgoingConnection("bak", 8529)

  val in = Flow[VMessage]
    //.wireTap(println(_))
   // .map { bytes => VMessage(bytes) }
    //.wireTap(println(_))
    .flatMapMerge(3, m => Source(m.chunks()))
//    .mapConcat(_.chunks())
    .wireTap(println(_))
    .map { chunk =>
      ByteString.fromArrayUnsafe(VChunk.codec.encode(chunk).require.toByteArray)
    }
    .prepend(Source.single(ByteString("VST/1.1\r\n\r\n")))

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
    .wireTap(println(_))
    .via(new VChunkMessageStage)
    .wireTap(println(_))
    .map { m => VResponse.from(m.id, m.data.bits).require }
    .wireTap(println(_))

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
      val message = VMessage(b)
      val promise = Promise[VResponse]()
      promises.update(message.id, promise)
      conn.offer(message).flatMap({
        case QueueOfferResult.Enqueued => promise.future
        case QueueOfferResult.Dropped => Future.failed(new RuntimeException("queue drop"))
        case QueueOfferResult.Failure(cause) => Future.failed(cause)
        case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("queue closed"))
      }) pipeTo sender()
    }
    case r: VResponse => {
      promises.remove(r.messageId).foreach { promise =>
        promise.success(r)
      }
    }
    case _ =>
  }
}

object VClient {
  case object MEND
}