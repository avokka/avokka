package example

import java.nio.{ByteBuffer, ByteOrder}

import org.scalamari.velocypack4s.core._
import org.scalamari.velocypack4s.macros._
import com.arangodb.velocypack._
import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.util._

import scala.util.{Failure, Success}
import scala.concurrent._
import scala.concurrent.duration._
import scala.io.StdIn._
import scodec._
import scodec.bits._
import scodec.codecs._

case class Foo(bar: String)

object Foo {
  val vpackSerializer: VPackSerializer[Foo] = serializer[Foo]
  val vpackDeserializer: VPackDeserializer[Foo] = deserializer[Foo]
}

object FooModule extends VPackModule {

  override def setup[C <: VPackSetupContext[C]](context: C): Unit = {
    context.registerSerializer(classOf[Foo], Foo.vpackSerializer)
    context.registerDeserializer(classOf[Foo], Foo.vpackDeserializer)
  }

}

case class VChunk
(
  length: Long,
  chunkX: Long,
  messageId: Long,
  messageLength: Long,
  data: ByteVector
)

object VChunk
{
  implicit val encoder: Codec[VChunk] = {
    ("length" | uint32L) ::
    ("chunkX" | uint32L) ::
    ("messageId" | int64L) ::
    ("messageLength" | int64L) ::
    ("data" | bytes)
  }.as[VChunk]
}

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()
//  import system.dispatcher

  def main(args: Array[String]): Unit = {

    val vpack = new VPack.Builder().registerModule(FooModule).build()

    val connection = Tcp().outgoingConnection("bak", 8529)

    val in = Flow[VChunk]
      .map { chunk =>
        VChunk.encoder.encode(chunk).require.toByteArray
      }
      .map(ByteString(_))
      .prepend(Source.single(ByteString("VST/1.0\r\n\r\n")))
      .wireTap { bs => println(bs) }

    val out = Flow[ByteString]
      .wireTap { bs => println(bs) }
      .via(Framing.lengthField(fieldLength = 4, maximumFrameLength = Int.MaxValue / 2))
      .map { bs =>
        println(bs)
        VChunk.encoder.decodeValue(BitVector(bs))
      }

    val r = vpack.serialize(Array(1, 1, None, 1, "/_api/version"))
    val chunk = VChunk(
      24 + r.getByteSize,
      3,
      1,
      r.getByteSize,
      ByteVector(r.getBuffer)
    )

    val testInput = Source(List(chunk)).concat(Source.maybe)

//    val out = connection.via(echo).via(framing).runWith(testInput, Sink.foreach(bs => println("client received: " + bs.utf8String)))
    val gr: Future[Done] = testInput.via(in).via(connection).async.via(out)
      .runWith(Sink.foreach(bs => println("client received: " + bs.require)))

    Await.result(gr, 10.seconds) //out.onComplete(_ => system.terminate())
  }
//  readLine()
}
