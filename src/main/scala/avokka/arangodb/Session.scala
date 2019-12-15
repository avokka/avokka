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

//  val vp = new VPack.Builder().build()
//  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)

  def askClient[T](t: T)(implicit encoder: Encoder[T]): EitherT[Future, VPackError, VStreamMessage] = for {
    request <- EitherT.fromEither[Future](encoder.encode(t).toEither.leftMap(VPackError.Codec))
    msg <- EitherT.liftF(ask(client, request.bytes).mapTo[VStreamMessage])
  } yield msg

  def exec[T <: Request[_], O](request: T)(implicit encoder: Encoder[T], decoder: VPackDecoder[O]): EitherT[Future, VPackError, Response[O]] = for {
    message <- askClient(request)
    response <- EitherT.fromEither[Future](Response.decode[O](message.data.bits))
  } yield response

  val _system = new Database(this, "_system")

  def authenticate(user: String, password: String): EitherT[Future, VPackError, Response[ResponseError]] = {
    exec[Request[Unit], ResponseError](Request(RequestHeader.Authentication(
      encryption = "plain", user = user, password = password
    ), ()))
  }

  def version(details: Boolean = false) = {
    exec[Request[Unit], api.Version](Request(RequestHeader.Header(
      database = _system.name,
      requestType = RequestType.GET,
      request = "/_api/version",
      parameters = Map("details" -> details.toString)
    ), ())).value
  }

  def databaseCreate(t: api.DatabaseCreate) = {
    exec[Request[api.DatabaseCreate], api.DatabaseCreate.Response](Request(RequestHeader.Header(
      database = _system.name,
      requestType = RequestType.POST,
      request = "/_api/database"
    ), t)).value
  }

  def databaseDrop(name: DatabaseName) = {
    exec[Request[Unit], api.DatabaseDrop](Request(RequestHeader.Header(
      database = _system.name,
      requestType = RequestType.DELETE,
      request = s"/_api/database/$name",
    ), ())).value
  }

  def databases() = {
    exec[Request[Unit], api.DatabaseList](Request(RequestHeader.Header(
      database = _system.name,
      requestType = RequestType.GET,
      request = "/_api/database/user",
    ), ())).value
  }

  def adminEcho() = {
    exec[Request[Unit], api.admin.AdminEcho](Request(RequestHeader.Header(
      database = _system.name,
      requestType = RequestType.POST,
      request = "/_admin/echo"
    ), ())).value
  }

  def adminLog() = {
    exec[Request[Unit], api.admin.AdminLog](Request(RequestHeader.Header(
      database = _system.name,
      requestType = RequestType.GET,
      request = "/_admin/log"
    ), ())).value
  }
}
