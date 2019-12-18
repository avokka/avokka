package avokka.arangodb

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import avokka.velocypack._
import avokka.velocystream._
import cats.data.EitherT
import cats.instances.future._
import scodec.bits.BitVector

import scala.concurrent.Future
import scala.concurrent.duration._

class Session(host: String, port: Int = 8529)(implicit system: ActorSystem, materializer: ActorMaterializer) extends ApiContext[Session] {

  lazy val session = this

  private val client = system.actorOf(Props(classOf[VStreamClient], host, port, materializer), name = s"velocystream-client")

  implicit val timeout: Timeout = Timeout(2.minutes)
  import system.dispatcher

  def askClient[T](bits: BitVector): FEE[VStreamMessage] = EitherT.liftF(ask(client, bits.bytes).mapTo[VStreamMessage])

  def exec[O](head: Request.HeaderTrait)(implicit decoder: VPackDecoder[O]): FEE[Response[O]] = {
    for {
      bits   <- EitherT.fromEither[Future](head.toVPackBits)
      message <- askClient(bits)
      response <- EitherT.fromEither[Future](Response.decode[O](message.data.bits))
    } yield response
  }

  def exec[P, O](request: Request[P])(implicit encoder: VPackEncoder[P], decoder: VPackDecoder[O]): FEE[Response[O]] = {
    for {
      hBits   <- EitherT.fromEither[Future](request.header.toVPackBits)
      pBits   <- EitherT.fromEither[Future](request.body.toVPackBits)
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
