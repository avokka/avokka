package avokka.arangodb

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import avokka.velocypack._
import avokka.velocystream._
import cats.data.Validated
import com.arangodb.velocypack.{VPack, VPackSlice}
import scodec.bits.BitVector
import scodec.{Decoder, Encoder}

import scala.concurrent.Future
import scala.concurrent.duration._

class Session(host: String, port: Int = 8529)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  private val client = system.actorOf(Props(classOf[VStreamClient], host, port, materializer), name = s"velocystream-client")

  implicit val timeout: Timeout = Timeout(2.minutes)
  import system.dispatcher

  val vp = new VPack.Builder().build()
  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)

  def askClient[T: Encoder](t: T): Future[VStreamMessage] = {
    val request = t.toVPack.valueOr(throw _)
   // println(toSlice(request.bits))
    ask(client, request).mapTo[VStreamMessage]
  }

  def authenticate(user: String, password: String): Future[Validated[VPackError, Response[AuthResponse]]] = {
    askClient(AuthRequest(1, MessageType.Authentication, "plain", user, password)).map { msg =>
      msg.data.bits.fromVPack[Response[AuthResponse]].map(_.value)
    }
  }

  def exec[T, O](request: Request[T])(implicit encoder: Encoder[T], decoder: Decoder[O]): Future[Validated[VPackError, Response[O]]] = {
    askClient(request).map { msg =>
      println(toSlice(msg.data.bits))
      msg.data.bits.fromVPack[Response[O]].map(_.value)
    }
  }

}
