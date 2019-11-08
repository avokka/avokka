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
      .map { chunk =>
        VChunk.codec.encode(chunk).require.toByteArray
      }
      .map(ByteString(_))
      .prepend(Source.single(ByteString("VST/1.0\r\n\r\n")))

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
      .map { ch =>
        vpack.deserialize[VResponse](new VPackSlice(ch.data.toArray), VResponse.getClass)
      }

    val r = vpack.serialize(Array(1, 1, None, 1, "/_api/version"))
    val chunk = VChunk(
      24 + r.getByteSize,
      3,
      1,
      r.getByteSize,
      ByteVector(r.getBuffer)
    )

    val testInput = Source.single(chunk)

    val gr: Future[Done] = testInput.via(in).via(connection).via(out)
      .runWith(Sink.foreach(bs => println("client received: " + bs)))

    Await.ready(gr, 10.seconds)

    Await.ready(system.terminate(), 1.minute)
  }

}
