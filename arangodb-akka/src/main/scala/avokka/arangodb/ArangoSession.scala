package avokka.arangodb

import akka.actor.ActorSystem
import akka.pattern.ask
import avokka.arangodb.protocol.{ArangoClient, ArangoRequest}
import avokka.velocypack._
import avokka.velocystream._
import cats.instances.future._
import cats.syntax.applicative._
import org.typelevel.log4cats.MessageLogger
import scodec.bits.ByteVector

import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.Future

trait ArangoSession extends ArangoClient[Future] {
  def closeClient(): Unit
}

object ArangoSession {

  private val vstClientId = new AtomicLong()
  private val vstMessageId = new AtomicLong()

  def apply(configuration: ArangoConfiguration)(
    implicit actorSystem: ActorSystem
  ): ArangoSession = {

    import actorSystem.dispatcher

    implicit val akkaLogger: MessageLogger[Future] = new MessageLogger[Future] {
      override def error(message: => String): Future[Unit] = actorSystem.log.error(message).pure
      override def warn(message: => String): Future[Unit] = actorSystem.log.warning(message).pure
      override def info(message: => String): Future[Unit] = actorSystem.log.info(message).pure
      override def debug(message: => String): Future[Unit] = actorSystem.log.debug(message).pure
      override def trace(message: => String): Future[Unit] = actorSystem.log.debug(message).pure
    }

    new ArangoClient.Impl(configuration) with ArangoSession {

      val authRequest = ArangoRequest.Authentication(user = configuration.username, password = configuration.password)
        .toVPackBits
        .map { bits =>
          VStreamMessage(vstMessageId.incrementAndGet(), bits.bytes)
        }
        .toOption

      private val vstClient = actorSystem.actorOf(
        VStreamClient(configuration, authRequest),
        name = s"velocystream-client-${vstClientId.incrementAndGet()}"
      )

      override protected def send(message: ByteVector): Future[ByteVector] = {
        val vstMessage = VStreamMessage(vstMessageId.incrementAndGet(), message)
        ask(vstClient, VStreamClient.MessageSend(vstMessage))(configuration.replyTimeout)
          .mapTo[VStreamMessage]
          .map(_.data)
      }

      override def closeClient(): Unit =
        vstClient ! VStreamClient.Stop

    }
  }

}
