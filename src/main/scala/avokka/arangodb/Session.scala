package avokka.arangodb

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import avokka.velocypack
import avokka.velocypack._
import avokka.velocystream._
import cats.data.EitherT
import cats.implicits._
import scodec.bits.BitVector

import scala.concurrent.Future
import scala.concurrent.duration._

class Session(host: String, port: Int = 8529)(implicit system: ActorSystem, materializer: ActorMaterializer) {
  import velocypack.codecs.vpackEncoder

  private val client = system.actorOf(Props(classOf[VStreamClient], host, port, materializer), name = s"velocystream-client")

  implicit val timeout: Timeout = Timeout(2.minutes)
  import system.dispatcher

  def askClient[T](bits: BitVector): FEE[VStreamMessage] = EitherT.liftF(ask(client, bits.bytes).mapTo[VStreamMessage])

  def exec[O](head: Request.HeaderTrait)(implicit decoder: VPackDecoder[O]): FEE[Response[O]] = {
    val header = Request.headerEncoder.encode(head)
    for {
      bits   <- EitherT.fromEither[Future](vpackEncoder.encode(header).toEither.leftMap(VPackError.Codec))
      message <- askClient(bits)
      response <- EitherT.fromEither[Future](Response.decode[O](message.data.bits))
    } yield response
  }

  def exec[P, O](request: Request[P])(implicit encoder: VPackEncoder[P], decoder: VPackDecoder[O]): FEE[Response[O]] = {
    val header = Request.headerEncoder.encode(request.header)
    val payload = encoder.encode(request.body)
    for {
      hBits   <- EitherT.fromEither[Future](vpackEncoder.encode(header).toEither.leftMap(VPackError.Codec))
      pBits   <- EitherT.fromEither[Future](vpackEncoder.encode(payload).toEither.leftMap(VPackError.Codec))
      message <- askClient(hBits ++ pBits)
      response <- EitherT.fromEither[Future](Response.decode[O](message.data.bits))
    } yield response
  }

  val _system = new Database(this, "_system")

  def authenticate(user: String, password: String) = {
    exec[ResponseError](Request.Authentication(
      encryption = "plain", user = user, password = password
    )).value
  }

  def version(details: Boolean = false) = {
    exec[api.Version](Request.Header(
      database = _system.name,
      requestType = RequestType.GET,
      request = "/_api/version",
      parameters = Map("details" -> details.toString)
    )).value
  }

  def databaseCreate(t: api.DatabaseCreate) = {
    exec[api.DatabaseCreate, api.DatabaseCreate.Response](Request(Request.Header(
      database = _system.name,
      requestType = RequestType.POST,
      request = "/_api/database"
    ), t)).value
  }

  def databaseDrop(name: DatabaseName) = {
    exec[api.DatabaseDrop](Request.Header(
      database = _system.name,
      requestType = RequestType.DELETE,
      request = s"/_api/database/$name",
    )).value
  }

  def databases() = {
    exec[api.DatabaseList](Request.Header(
      database = _system.name,
      requestType = RequestType.GET,
      request = "/_api/database/user",
    )).value
  }

  def adminEcho() = {
    exec[api.admin.AdminEcho](Request.Header(
      database = _system.name,
      requestType = RequestType.POST,
      request = "/_admin/echo"
    )).value
  }

  def adminLog() = {
    exec[api.admin.AdminLog](Request.Header(
      database = _system.name,
      requestType = RequestType.GET,
      request = "/_admin/log"
    )).value
  }
}
