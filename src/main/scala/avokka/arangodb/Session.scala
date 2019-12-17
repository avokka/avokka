package avokka.arangodb

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import avokka.velocypack
import avokka.velocypack._
import avokka.velocystream._
import cats.Show
import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import scodec.bits.BitVector

import scala.concurrent.Future
import scala.concurrent.duration._

class Session(host: String, port: Int = 8529)(implicit system: ActorSystem, materializer: ActorMaterializer) extends ApiContext[Session] {
  import velocypack.codecs.vpackEncoder

  lazy val session = this

  private val client = system.actorOf(Props(classOf[VStreamClient], host, port, materializer), name = s"velocystream-client")

  implicit val timeout: Timeout = Timeout(2.minutes)
  import system.dispatcher

  def askClient[T](bits: BitVector): FEE[VStreamMessage] = EitherT.liftF(ask(client, bits.bytes).mapTo[VStreamMessage])

  def exec[O](head: Request.HeaderTrait)(implicit decoder: VPackDecoder[O]): FEE[Response[O]] = {
    val header = Request.headerEncoder.encode(head)
//    println("request header", Show[VPack].show(header))
    for {
      bits   <- EitherT.fromEither[Future](vpackEncoder.encode(header).toEither.leftMap(VPackError.Codec))
      message <- askClient(bits)
      response <- EitherT.fromEither[Future](Response.decode[O](message.data.bits))
    } yield response
  }

  def exec[P, O](request: Request[P])(implicit encoder: VPackEncoder[P], decoder: VPackDecoder[O]): FEE[Response[O]] = {
    val header = Request.headerEncoder.encode(request.header)
//    println("request header", Show[VPack].show(header))
    val payload = encoder.encode(request.body)
//    println("request payload", Show[VPack].show(payload))
    for {
      hBits   <- EitherT.fromEither[Future](vpackEncoder.encode(header).toEither.leftMap(VPackError.Codec))
      pBits   <- EitherT.fromEither[Future](vpackEncoder.encode(payload).toEither.leftMap(VPackError.Codec))
      message <- askClient(hBits ++ pBits)
      response <- EitherT.fromEither[Future](Response.decode[O](message.data.bits))
    } yield response
  }

  lazy val _system = new Database(this, Database.systemName)

  def authenticate(user: String, password: String) = {
    exec[ResponseError](Request.Authentication(
      encryption = "plain", user = user, password = password
    )).value
  }

  def databaseCreate(t: api.DatabaseCreate) = {
    exec[api.DatabaseCreate, api.DatabaseCreate.Response](Request(Request.Header(
      database = Database.systemName,
      requestType = RequestType.POST,
      request = "/_api/database"
    ), t)).value
  }

  def databaseDrop(name: DatabaseName) = {
    exec[api.DatabaseDrop](Request.Header(
      database = Database.systemName,
      requestType = RequestType.DELETE,
      request = s"/_api/database/$name",
    )).value
  }

  def databases() = {
    exec[api.DatabaseList](Request.Header(
      database = Database.systemName,
      requestType = RequestType.GET,
      request = "/_api/database/user",
    )).value
  }

  def adminEcho() = {
    exec[api.admin.AdminEcho](Request.Header(
      database = Database.systemName,
      requestType = RequestType.POST,
      request = "/_admin/echo"
    )).value
  }

  def adminLog() = {
    exec[api.admin.AdminLog](Request.Header(
      database = Database.systemName,
      requestType = RequestType.GET,
      request = "/_admin/log"
    )).value
  }
}
