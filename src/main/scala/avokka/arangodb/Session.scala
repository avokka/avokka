package avokka.arangodb

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import avokka.velocypack._
import avokka.velocystream._
import cats.data.{EitherT, Validated}
import com.arangodb.velocypack.{VPack, VPackSlice}
import scodec.bits.BitVector
import scodec.{Decoder, Encoder}
import cats.implicits._

import scala.concurrent.Future
import scala.concurrent.duration._

class Session(host: String, port: Int = 8529)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  private val client = system.actorOf(Props(classOf[VStreamClient], host, port, materializer), name = s"velocystream-client")

  implicit val timeout: Timeout = Timeout(2.minutes)
  import system.dispatcher

  val vp = new VPack.Builder().build()
  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)

  def askClient[T: Encoder](t: T): EitherT[Future, VPackError, VStreamMessage] = for {
    request <- EitherT.fromEither[Future](t.toVPack)
    msg <- EitherT.liftF(ask(client, request).mapTo[VStreamMessage])
  } yield msg

  def authenticate(user: String, password: String): EitherT[Future, VPackError, Response[ResponseError]] = for {
    message <- askClient(RequestAuthentication(encryption = "plain", user = user, password = password))
    response <- EitherT.fromEither[Future](Response.decode[ResponseError](message.data.bits))
  } yield response

  def exec[T, O](request: Request[T])(implicit encoder: Encoder[T], decoder: Decoder[O]): EitherT[Future, VPackError, Response[O]] = for {
    message <- askClient(request)
    response <- EitherT.fromEither[Future](Response.decode[O](message.data.bits))
  } yield response

  val _system = new Database(this, "_system")

  def databases() = {
    exec[Unit, api.Database](Request(RequestHeader(
      database = _system.database,
      requestType = RequestType.GET,
      request = "/_api/database",
    ), ())).value
  }

  def adminEcho() = {
    exec[Unit, api.admin.AdminEcho](Request(RequestHeader(
      database = _system.database,
      requestType = RequestType.POST,
      request = "/_admin/echo"
    ), ())).value
  }

  def adminLog() = {
    exec[Unit, api.admin.AdminLog](Request(RequestHeader(
      database = _system.database,
      requestType = RequestType.GET,
      request = "/_admin/log"
    ), ())).value
  }
}
