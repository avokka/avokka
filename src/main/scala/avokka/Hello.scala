package avokka

import java.nio.ByteOrder

import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.util._
import avokka.velocypack.codecs.VPackObjectCodec
import scodec.bits._

import scala.concurrent._
import scala.concurrent.duration._

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val connection = Tcp().outgoingConnection("bak", 8529)

    val in = Flow[ByteVector]
      .wireTap(println(_))
      .map { bytes => VChunk(bytes) }
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
      .map { chunk => VResponse.from(chunk.messageId, chunk.data.bits) }
      //.map { mes => new VPackSlice(ch.data.toArray)}
      .wireTap(println(_))
      .wireTap { r =>
        println(VPackObjectCodec.decode(r.require.body))
      }
      /*
      .map { vp =>
        vpack.deserialize[VResponse](vp, classOf[VResponse])
      }
*/

    val auth = VAuthRequest.codec.encode(VAuthRequest(1, 1000, "plain", "root", "root")).require.bytes
    val apiV = VRequest.codec.encode(VRequest(1, 1, "_system", 1, "/_api/version", meta = Map("test" -> "moi"))).require.bytes

    val testInput = Source(List(auth, apiV))

    val gr: Future[Done] = testInput.via(in).via(connection).via(out)
      .runWith(Sink.ignore)

    Await.ready(gr, 10.seconds)

    Await.ready(system.terminate(), 1.minute)
  }



}
