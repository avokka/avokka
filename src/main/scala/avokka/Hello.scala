package avokka

import akka.actor._
import akka.stream._
import avokka.velocypack._
import avokka.velocystream._

import scala.concurrent._
import scala.concurrent.duration._

object Hello {

  implicit val system = ActorSystem("avokka")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val client = system.actorOf(Props(classOf[VClient], materializer), name = s"vst-client")

    val auth = VAuthRequest(1, 1000, "plain", "root", "root").toVPack.valueOr(throw _)
    client ! auth

    val apiV = VRequestHeader(1, 1, "_system", 1, "/_api/version", meta = Map("test" -> "moi")).toVPack.valueOr(throw _)
    client ! apiV

    /*
    val testInput = Source(List(auth, apiV))

    val gr: Future[Done] = testInput.via(in).via(connection).via(out)
      .runWith(Sink.ignore)

    Await.ready(gr, 10.seconds)
    */

    Thread.sleep(200)

    Await.ready(system.terminate(), 1.minute)
  }



}
