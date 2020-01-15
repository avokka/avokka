import java.net.InetSocketAddress

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl._
import akka.util.ByteString
import avokka.arangodb.{ArangoRequest, DatabaseName, RequestType}
import avokka.velocystream.{VStreamChunk, VStreamMessage}
import avokka.velocypack._
import scodec.bits.BitVector

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable

object StreamNetFailTest {
  val bytes = ArangoRequest.Header(
    database = DatabaseName.system,
    requestType = RequestType.GET,
    request = "/_api/version",
  ).toVPackBits.getOrElse(BitVector.empty).bytes

  val promises: mutable.LongMap[Promise[ByteString]] = mutable.LongMap.empty

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val address = InetSocketAddress.createUnresolved("bak", 8529)

  val tcpConnection =
    Tcp().outgoingConnection(address, connectTimeout = 5.seconds, idleTimeout = 1.minute)

  val protocol = RestartFlow.withBackoff(
    minBackoff = 2.seconds,
    maxBackoff = 30.seconds,
    randomFactor = 0.2,
    maxRestarts = -1
  )( () => { Flow[VStreamMessage]
    .flatMapMerge(2, m => Source(m.chunks()))
    .map(chunk => ByteString.fromArrayUnsafe(VStreamChunk.codec.encode(chunk).require.toByteArray) )
    .prepend(Source.single(ByteString("VST/1.1\r\n\r\n")))
    .log("SEND")
    .via(tcpConnection)
    .log("RECV")
  } )

  val sourceQueue = Source.queue[VStreamMessage](10, OverflowStrategy.backpressure)

  val graph: SourceQueueWithComplete[VStreamMessage] = sourceQueue.via(protocol)
    .to(Sink.foreach(bs => println("RECV", bs.utf8String))).run()

  def send(m: VStreamMessage) = {
    val promise = Promise[ByteString]()
    promises.update(m.id, promise)
    graph.offer(m)
      .map({
        case QueueOfferResult.Enqueued       => promise
        case QueueOfferResult.Dropped        => promise.failure(new RuntimeException("queue drop"))
        case QueueOfferResult.Failure(cause) => promise.failure(cause)
        case QueueOfferResult.QueueClosed    => promise.failure(new RuntimeException("queue closed"))
      })
      .flatMap(_.future)
  }

  def main(args: Array[String]): Unit = {

    Source.tick(1.second, 5.seconds, NotUsed)
      .map(_ => VStreamMessage.create(bytes))
      .via(protocol)
      .to(Sink.foreach(b => println(b.utf8String)))
      .run()
  }
}
