package avokka

import java.nio.ByteOrder

import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.util._
import avokka.velocypack._
import avokka.velocypack.codecs.VPackObjectCodec
import avokka.velocystream._
import scodec.bits._

import scala.concurrent._
import scala.concurrent.duration._

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val connection = Tcp().outgoingConnection("bak", 8529)

    val in = Flow[ByteVector]
      //.wireTap(println(_))
      .map { bytes => VMessage(bytes) }
      .wireTap(println(_))
      .mapConcat { msg =>
        msg.chunks()
      }
      .wireTap(println(_))
      .map { chunk =>
        ByteString.fromArrayUnsafe(VChunk.codec.encode(chunk).require.toByteArray)
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
        println(r.require.body.fromVPack(VPackObjectCodec))
      }


    val auth = VAuthRequest(1, 1000, "plain", "root", "root").toVPack.valueOr(throw _)
    val apiV = VRequestHeader(1, 1, "_system", 1, "/_api/version", meta = Map("test" -> "moi")).toVPack.valueOr(throw _)

    val testInput = Source(List(auth, apiV))

    val gr: Future[Done] = testInput.via(in).via(connection).via(out)
      .runWith(Sink.ignore)

    Await.ready(gr, 10.seconds)

    Await.ready(system.terminate(), 1.minute)
  }



}
