package avokka

import akka.actor._
import akka.stream._
import avokka.velocypack._
import avokka.velocystream._
import akka.pattern.ask
import akka.util.Timeout
import avokka.velocypack.codecs.VPackObjectCodec

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val client = system.actorOf(Props(classOf[VClient], materializer), name = s"vst-client")

    implicit val timeout = Timeout(5.seconds)

    val auth = ask(client, VAuthRequest(1, 1000, "plain", "root", "root").toVPack.valueOr(throw _))
    val version = ask(client, VRequestHeader(1, 1, "_system", 1, "/_api/version", meta = Map("test" -> "moi")).toVPack.valueOr(throw _))

    val r = for {
      authR <- auth.mapTo[VResponse]
      a = authR.body.fromVPack(VPackObjectCodec)
      versionR <- version.mapTo[VResponse]
      v = versionR.body.fromVPack(VPackObjectCodec)
    } yield (a, v)

    println(Await.result(r, 10.seconds))
    /*
    val testInput = Source(List(auth, apiV))

    val gr: Future[Done] = testInput.via(in).via(connection).via(out)
      .runWith(Sink.ignore)

    Await.ready(gr, 10.seconds)
    */

   // Thread.sleep(200)

    Await.ready(system.terminate(), 1.minute)
  }



}
