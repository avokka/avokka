package avokka.arangodb

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import avokka.velocypack._
import avokka.velocystream._
import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import scodec.bits.BitVector

import scala.concurrent.Future
import scala.concurrent.duration._

class Session(host: String, port: Int = 8529)(
    implicit val system: ActorSystem,
    materializer: ActorMaterializer
) extends ApiContext[Session] {

  override lazy val session: Session = this

  lazy val _system = new Database(this, DatabaseName.system)

  private val client = system.actorOf(Props(classOf[VStreamClient], host, port, materializer),
                                      name = s"velocystream-client")

  implicit val timeout: Timeout = Timeout(2.minutes)
  import system.dispatcher

  def askClient[T](bits: BitVector): FEE[VStreamMessage] =
    EitherT.liftF(ask(client, bits.bytes).mapTo[VStreamMessage])

  private[arangodb] def execute[P: VPackEncoder, O: VPackDecoder](
      request: Request[P]): FEE[Response[O]] = {
    for {
      hBits <- EitherT.fromEither[Future](request.header.toVPackBits.leftMap(ArangoError.VPack))
      pBits <- EitherT.fromEither[Future](request.body.toVPackBits.leftMap(ArangoError.VPack))
      message <- askClient(hBits ++ pBits)
      response <- EitherT.fromEither[Future](Response.decode[O](message.data.bits))
    } yield response
  }

}
