package avokka

import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong

import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.util._
import com.arangodb.velocypack._
import scodec.bits._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Random

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val vpack = new VPack.Builder().build()

    val connection = Tcp().outgoingConnection("bak", 8529)

    val messageId = new AtomicLong()

    val in = Flow[VPackSlice]
      .wireTap(println(_))
      .map { slice => ByteVector.view(slice.getBuffer, slice.getStart, slice.getByteSize) }
      .wireTap(println(_))
      .map { bytes => VChunk(messageId.incrementAndGet(), bytes)}
      .wireTap(println(_))
      .map { chunk =>
        ByteString(VChunk.codec.encode(chunk).require.toByteBuffer)
      }
   //   .wireTap(println(_))
      //.map(ByteString(_))
      .prepend(Source.single(ByteString("VST/1.1\r\n\r\n")))

    val out = Flow[ByteString]
      .via(Framing.lengthField(
        fieldLength = 4,
        fieldOffset = 0,
        maximumFrameLength = Int.MaxValue,
        byteOrder = ByteOrder.LITTLE_ENDIAN,
        computeFrameSize = (_, l) => l
      ))
//      .wireTap(println(_))
      .map { bs =>
        VChunk.codec.decodeValue(BitVector(bs)).require
      }
      .wireTap(println(_))
      .map { chunk => VMessage(chunk.messageId, chunk.data)}
      //.map { mes => new VPackSlice(ch.data.toArray)}
      .wireTap(println(_))
      /*
      .map { vp =>
        vpack.deserialize[VResponse](vp, classOf[VResponse])
      }
*/

    val auth = vpack.serialize(Array(1, 1000, "plain", "root", "root"))
    val apiVersion = vpack.serialize(Array(1, 1, "_system", 1, "/_api/version", new Object, new Object))

    val testInput = Source(List(auth, apiVersion))

    val gr: Future[Done] = testInput.via(in).via(connection).via(out)
      .runWith(Sink.ignore)

    Await.ready(gr, 10.seconds)

    Await.ready(system.terminate(), 1.minute)
  }



}
