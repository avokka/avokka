package avokka.velocystream

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import avokka.velocypack._
import cats.data.Validated
import scodec.{Attempt, Codec, Decoder, Encoder}

import scala.concurrent.Future
import scala.concurrent.duration._

class VSession(host: String, port: Int = 8529)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  private val client = system.actorOf(Props(classOf[VClient], host, port, materializer), name = s"velocystream-client")

  implicit val timeout: Timeout = Timeout(2.minutes)
  import system.dispatcher

  def askClient[T: Encoder](t: T): Future[VMessage] = {
    ask(client, t.toVPack.valueOr(throw _)).mapTo[VMessage]
  }

  def authenticate(user: String, password: String): Future[Validated[VPackError, VResponse[VAuthResponse]]] = {
    askClient(VAuthRequest(1, 1000, "plain", user, password)).map { msg =>
      msg.data.bits.fromVPack[VResponse[VAuthResponse]].map(_.value)
    }
  }

  def exec[T, O](request: VRequest[T])(implicit encoder: Encoder[T], decoder: Decoder[O]): Future[Validated[VPackError, VResponse[O]]] = {
    askClient(request).map { msg =>
      msg.data.bits.fromVPack[VResponse[O]].map(_.value)
    }
  }

}
