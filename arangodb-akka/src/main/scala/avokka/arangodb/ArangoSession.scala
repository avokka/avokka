package avokka.arangodb

import akka.actor.ActorSystem
import akka.pattern.ask
import avokka.arangodb.protocol.{ArangoProtocol, ArangoRequest}
import avokka.velocypack._
import avokka.velocystream._
import cats.instances.future._
import cats.syntax.applicative._
import org.typelevel.log4cats.MessageLogger

import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.Future

trait ArangoSession extends ArangoProtocol[Future] {
  def closeClient(): Unit
}

object ArangoSession {

  val id = new AtomicLong()

  def apply(configuration: ArangoConfiguration)(
    implicit system: ActorSystem
  ): ArangoSession = {

    import system.dispatcher

    implicit val akkaLogger: MessageLogger[Future] = new MessageLogger[Future] {
      override def error(message: => String): Future[Unit] = system.log.error(message).pure
      override def warn(message: => String): Future[Unit] = system.log.warning(message).pure
      override def info(message: => String): Future[Unit] = system.log.info(message).pure
      override def debug(message: => String): Future[Unit] = system.log.debug(message).pure
      override def trace(message: => String): Future[Unit] = system.log.debug(message).pure
    }

    new ArangoProtocol.Impl[Future](configuration) with ArangoSession {

      val authRequest = ArangoRequest.Authentication(user = configuration.username, password = configuration.password).toVPackBits
      val authSeq = authRequest.map(bits => VStreamMessage.create(bits.bytes)).toOption

      private val vstClient = system.actorOf(
        VStreamClient(configuration, authSeq),
        name = s"velocystream-client-${ArangoSession.id.incrementAndGet()}"
      )

      override protected def send(message: VStreamMessage): Future[VStreamMessage] = {
        ask(vstClient, VStreamClient.MessageSend(message))(configuration.replyTimeout).mapTo[VStreamMessage]
      }

      override def closeClient(): Unit =
        vstClient ! VStreamClient.Stop

    }
  }

}
