package avokka.arangodb
package akka

import _root_.akka.actor.ActorSystem
import _root_.akka.pattern.ask
import avokka.arangodb.protocol._
import avokka.velocypack._
import avokka.velocystream._
import cats.instances.future._
import cats.syntax.applicative._
import org.typelevel.log4cats.MessageLogger
import scodec.bits.ByteVector

import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.Future

trait Arango extends ArangoClient[Future] {
  def closeClient(): Unit
}

object Arango {

  private val vstClientId = new AtomicLong()
  private val vstMessageId = new AtomicLong()

  def apply(configuration: ArangoConfiguration)(
    implicit actorSystem: ActorSystem
  ): Arango = {

    import actorSystem.dispatcher

    implicit val akkaLogger: MessageLogger[Future] = new MessageLogger[Future] {
      override def error(message: => String): Future[Unit] = actorSystem.log.error(message).pure
      override def warn(message: => String): Future[Unit] = actorSystem.log.warning(message).pure
      override def info(message: => String): Future[Unit] = actorSystem.log.info(message).pure
      override def debug(message: => String): Future[Unit] = actorSystem.log.debug(message).pure
      override def trace(message: => String): Future[Unit] = actorSystem.log.debug(message).pure
    }

    new ArangoClient.Impl(configuration) with Arango {

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
