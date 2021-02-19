package avokka.arangodb

import akka.NotUsed
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.scaladsl.Source
import avokka.arangodb.protocol.{ArangoProtocol, ArangoRequest}
import avokka.arangodb.types.DatabaseName
import avokka.velocypack._
import avokka.velocystream._
import com.typesafe.scalalogging.StrictLogging

import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.{ExecutionContext, Future}

class ArangoSession(configuration: ArangoConfiguration)(
    implicit val system: ActorSystem, ec: ExecutionContext
) extends ArangoProtocol.Impl[Future] with ArangoStream[ArangoSession.AkkaStream, Future] with StrictLogging {

  val authRequest = ArangoRequest.Authentication(user = configuration.username, password = configuration.password).toVPackBits
  val authSeq = authRequest.map(bits => VStreamMessage.create(bits.bytes)).toOption

  /*
  private val client = system.actorOf(
    BackoffSupervisor.props(
      BackoffOpts.onStop(
        VStreamConnection(conf, authSeq),
        childName = "velocystream-connection",
        minBackoff = 10.millis,
        maxBackoff = 20.seconds,
        randomFactor = 0.1)),
    name = s"velocystream-client-${ArangoSession.id.incrementAndGet()}"
  )
*/
  private val vstClient = system.actorOf(
    VStreamClient(configuration, authSeq),
    name = s"velocystream-client-${ArangoSession.id.incrementAndGet()}"
  )

  override protected def send(message: VStreamMessage): Future[VStreamMessage] = {
    ask(vstClient, VStreamClient.MessageSend(message))(configuration.replyTimeout).mapTo[VStreamMessage]
  }

  @deprecated("TODO: move with configuration in ArangoClient")
  lazy val _system = client.database(DatabaseName.system)
  @deprecated("TODO: move with configuration in ArangoClient")
  lazy val db = client.database(configuration.database)


  def closeClient(): Unit =
    vstClient ! VStreamClient.Stop

  /*
  private[arangodb] def execute[P: VPackEncoder, O: VPackDecoder](
      request: ArangoRequest[P]): FEE[ArangoResponse[O]] = {
    (for {
      hBits <- EitherT.fromEither[Future](request.header.toVPackBits)
      _ = logger.debug("REQ head {}", hBits.asVPackValue.toTry.show)
      pBits <- EitherT.fromEither[Future](request.body.toVPackBits)
      _ = if (pBits.nonEmpty) logger.debug("REQ body {}", pBits.asVPackValue.toTry.show)
      message <- askClient(hBits ++ pBits)
      _ = logger.debug("RES head {}", message.data.bits.asVPackValue.toTry.show)
      head <- EitherT.fromEither[Future](message.data.bits.asVPack[Header])
      _ = if (head.remainder.nonEmpty) logger.debug("RES body {}", head.remainder.asVPackValue.toTry.show)
      response <- EitherT.fromEither[Future](if (head.remainder.isEmpty) {
        (ArangoError.Head(head.value): ArangoError).asLeft
      } else if (head.value.responseCode >= 400) {
        head.remainder
          .asVPack[ResponseError]
          //.leftMap(ArangoError.VPack)
          .flatMap(body => ArangoError.Resp(head.value, body.value).asLeft)
      } else {
        head.remainder
          .asVPack[O]
          //.leftMap(ArangoError.VPack)
          .flatMap(body => protocol.ArangoResponse(head.value, body.value).asRight)
      })
    } yield response).leftMap { err =>
      logger.error("arangodb error from request head=%s body=%s".format(request.header.toString, request.body.toString), err)
      err match {
        case a: ArangoError => a
        case e: VPackError => ArangoError.VPack(e)
        case e => ArangoError.Unknown(e)
      }
    }
  }

   */

  override def fromQuery[V, T: VPackDecoder](query: ArangoQuery[Future, V]): Source[T, NotUsed] = Source.fromGraph(
    new CursorSource(query)
  )

}

object ArangoSession {
  type AkkaStream[_[_], T] = Source[T, NotUsed]

  val id = new AtomicLong()
}
