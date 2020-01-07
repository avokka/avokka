package avokka.arangodb

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import avokka.arangodb
import avokka.arangodb.ArangoResponse.Header
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
      _ = println("arango REQ head", hBits.asVPack.show)
      pBits <- EitherT.fromEither[Future](request.body.toVPackBits.leftMap(ArangoError.VPack))
      _ = if (pBits.nonEmpty) println("arango REQ body", pBits.asVPack.show)
      message <- askClient(hBits ++ pBits)
//      _ = system.log.debug("arango response message {}", message.data.bits.asVPack.show)
      _ = println("arango RES head", message.data.bits.asVPack.show)
      head <- EitherT.fromEither[Future](message.data.bits.as[Header].leftMap(ArangoError.VPack))
      _ = if (head.remainder.nonEmpty) println("arango RES body", head.remainder.asVPack.show)
      response <- EitherT.fromEither[Future](if (head.remainder.isEmpty) {
        (ArangoError.Head(head.value): ArangoError).asLeft
      } else if (head.value.responseCode >= 400) {
        head.remainder
          .as[ResponseError]
          .leftMap(ArangoError.VPack)
          .flatMap(body => ArangoError.Resp(head.value, body.value).asLeft)
      } else {
        head.remainder
          .as[O]
          .leftMap(ArangoError.VPack)
          .flatMap(body => ArangoResponse(head.value, body.value).asRight)
      })

      //response <- EitherT.fromEither[Future](decode[O](message.data.bits))
    } yield response
  }

  /*
  def decode[T](bits: BitVector)(
    implicit bodyDecoder: VPackDecoder[T]
  ): Either[ArangoError, ArangoResponse[T]] = {
    println("arango RES head", bits.asVPack.show)
    bits.as[Header].leftMap(ArangoError.VPack).flatMap { header =>
      if (header.remainder.isEmpty) {
        ArangoError.Head(header.value).asLeft
      } else {
        println("arango RES body", header.remainder.asVPack.show)
        if (header.value.responseCode >= 400) {
          header.remainder
            .as[ResponseError]
            .leftMap(ArangoError.VPack)
            .flatMap(body => ArangoError.Resp(header.value, body.value).asLeft)
        } else {
          header.remainder
            .as[T]
            .leftMap(ArangoError.VPack)
            .flatMap(body => ArangoResponse(header.value, body.value).asRight)
        }
      }
    }
  }

   */

  apply(ArangoRequest.Authentication(user = conf.username, password = conf.password))
}

object ArangoSession {
  val id = new AtomicLong()
}