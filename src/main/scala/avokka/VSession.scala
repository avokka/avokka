package avokka

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import avokka.velocypack._
import avokka.velocystream._
import cats.data.Validated
import scodec.{Decoder, Encoder}
import com.arangodb.velocypack.{VPack, VPackSlice}
import scodec.bits.BitVector

import scala.concurrent.Future
import scala.concurrent.duration._

class VSession(host: String, port: Int = 8529)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  private val client = system.actorOf(Props(classOf[VClient], host, port, materializer), name = s"velocystream-client")

  implicit val timeout: Timeout = Timeout(2.minutes)
  import system.dispatcher

  val vp = new VPack.Builder().build()
  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)

  def askClient[T: Encoder](t: T): Future[VMessage] = {
    val request = t.toVPack.valueOr(throw _)
    println(toSlice(request.bits))
    ask(client, request).mapTo[VMessage]
  }

  def authenticate(user: String, password: String): Future[Validated[VPackError, VResponse[VAuthResponse]]] = {
    askClient(VAuthRequest(1, MessageType.Authentication, "plain", user, password)).map { msg =>
      msg.data.bits.fromVPack[VResponse[VAuthResponse]].map(_.value)
    }
  }

  def exec[T, O](request: VRequest[T])(implicit encoder: Encoder[T], decoder: Decoder[O]): Future[Validated[VPackError, VResponse[O]]] = {
    askClient(request).map { msg =>
      msg.data.bits.fromVPack[VResponse[O]].map(_.value)
    }
  }

}
