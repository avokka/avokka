package avokka.velocystream

import java.nio.ByteOrder

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import akka.util.ByteString
import avokka.velocypack._
import avokka.velocypack.codecs.VPackObjectCodec
import scodec.bits.{BitVector, ByteVector}
import GraphDSL.Implicits._

import scala.concurrent.Future

class VClient(implicit materializer: ActorMaterializer) extends Actor {
  import VClient._
  import context.system
  import context.dispatcher

  val VST = GraphDSL.create() { implicit builder =>
    val merge = builder.add(Concat[ByteString]())
    Source.single(ByteString("VST/1.1\r\n\r\n")) ~> merge.in(0)
    FlowShape(merge.in(1), merge.out)
  }.named("VST")

  val connection = Tcp().outgoingConnection("bak", 8529)

  val in = Flow[ByteVector]
    //.wireTap(println(_))
    .map { bytes => VMessage(bytes) }
    .wireTap(println(_))
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

  val q = Source.queue[ByteVector](100, OverflowStrategy.fail)

  val gr = q.via(in)
    //.groupBy(2, _.size > 30)
    .via(connection)
    //.mergeSubstreams
    .via(out)

  val conn = gr.to(Sink.actorRef(self, MEND)).run()

  override def receive: Receive = {
    case MEND => //context.stop(self)
    case b: ByteVector => conn.offer(b).map(sender() ! _)
    case r: VResponse => println(r.body.fromVPack(VPackObjectCodec))
    case _ =>
  }
}

object VClient {
  case object MEND
}