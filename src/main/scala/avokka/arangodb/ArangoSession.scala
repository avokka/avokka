package avokka.arangodb

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import avokka.velocypack._
import avokka.velocystream._
import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import scodec.bits.BitVector

import scala.concurrent.Future
import scala.concurrent.duration._

class ArangoSession(conf: ArangoConfiguration)(
    implicit val system: ActorSystem,
    materializer: Materializer
) extends ApiContext[ArangoSession] {

  override lazy val session: ArangoSession = this

  lazy val _system = new ArangoDatabase(this, DatabaseName.system)
  lazy val db = new ArangoDatabase(this, DatabaseName(conf.database))

  private val client = system.actorOf(Props(classOf[VStreamClient], conf.host, conf.port, materializer),
                                      name = s"velocystream-client-${ArangoSession.id.incrementAndGet()}")

  implicit val timeout: Timeout = Timeout(2.minutes)
  import system.dispatcher

  def askClient[T](bits: BitVector): FEE[VStreamMessage] =
    EitherT.liftF(ask(client, bits.bytes).mapTo[VStreamMessage])

  private[arangodb] def execute[P: VPackEncoder, O: VPackDecoder](
      request: ArangoRequest[P]): FEE[ArangoResponse[O]] = {
    for {
      hBits <- EitherT.fromEither[Future](request.header.toVPackBits.leftMap(ArangoError.VPack))
      pBits <- EitherT.fromEither[Future](request.body.toVPackBits.leftMap(ArangoError.VPack))
      message <- askClient(hBits ++ pBits)
      response <- EitherT.fromEither[Future](ArangoResponse.decode[O](message.data.bits))
    } yield response
  }

  session(ArangoRequest.Authentication(user = conf.username, password = conf.password))
}

object ArangoSession {
  val id = new AtomicLong()
}