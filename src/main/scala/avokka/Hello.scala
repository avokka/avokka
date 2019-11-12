package avokka

import java.nio.ByteOrder

import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.util._
import com.arangodb.velocypack._
import scodec.bits._

import scala.concurrent._
import scala.concurrent.duration._

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val vpack = new VPack.Builder().build()

    val connection = Tcp().outgoingConnection("bak", 8529)

    val in = Flow[VChunk]
      .wireTap(println(_))
      .map { chunk =>
        ByteString(VChunk.codec.encode(chunk).require.toByteArray)
      }
      .wireTap(println(_))
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
      .wireTap(println(_))
      .map { bs =>
        VChunk.codec.decodeValue(BitVector(bs)).require
      }
      .wireTap(println(_))
      .map { ch => new VPackSlice(ch.data.toArray)}
      .wireTap(println(_))
      .map { vp =>
        vpack.deserialize[VResponse](vp, classOf[VResponse])
      }

    val r = vpack.serialize(Array(1, 1, "_system", 1, "/_api/version", new Object, new Object))
    println(r.toString)
//    val rSize = r.getByteSize
//    val chunk = VChunk(1, ByteVector(r.getBuffer, r.getStart, rSize))
    val chunk = VChunk(1, ByteVector(r.getBuffer, r.getStart, r.getByteSize))
    // val chunk2 = VChunk(2, ByteVector(r.getBuffer, r.getStart, r.getByteSize))

    val testInput = Source.single(chunk).concat(Source.maybe)

    val gr: Future[Done] = testInput.via(in).via(connection).via(out)
      .runWith(Sink.foreach(bs => println("client received: " + bs)))

    Await.ready(gr, 10.seconds)

    Await.ready(system.terminate(), 1.minute)
  }

}
