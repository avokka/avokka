package avokka.arangodb
package fs2

import protocol.ArangoClient
import avokka.velocystream._
import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.syntax.bracket._
import cats.effect.syntax.concurrent._
import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Timer}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import _root_.fs2.Pipe
import _root_.fs2.concurrent.SignallingRef
import _root_.fs2.io.tcp._
import org.typelevel.log4cats.Logger
import scodec.bits.ByteVector

import java.net.InetSocketAddress

trait Arango[F[_]] extends ArangoClient[F] {
   def terminate: F[Unit]
}

object Arango {

  def apply[F[_]: ContextShift: Timer](
    config: ArangoConfiguration,
  )(implicit C: Concurrent[F], L: Logger[F]): Resource[F, Arango[F]] = {

    def in(responses: FMap[F, Long, Deferred[F, ByteVector]]): Pipe[F, VStreamMessage, Unit] = _.evalMapChunk { msg =>
      responses.remove(msg.id).flatMap {
        case Some(deferred) => deferred.complete(msg.data)
        case None           => C.raiseError[Unit](new Exception(s"unknown message id ${msg.id}"))
      }
    }

    def impl(client: Socket[F]): F[Arango[F]] = for {
      counter <- Ref.of(0L)
      responses <- FMap[F, Long, Deferred[F, ByteVector]]
      stateSignal <- SignallingRef[F, ConnectionState](ConnectionState.Disconnected)
      closeSignal <- SignallingRef[F, Boolean](false)
      socket <- VChunkSocket(config, client, stateSignal, closeSignal, in(responses))
      fib <- socket.pump.start
    } yield new ArangoClient.Impl[F](config) with Arango[F] {

      override def send(data: ByteVector): F[ByteVector] =
        for {
          id <- counter.updateAndGet(_ + 1)
          _ <- L.debug(s"prepare message #$id")
          dfr <- Deferred[F, ByteVector]
          _ <- responses.update(id, dfr)
          _ <- socket.send(VStreamMessage(id, data))
          r <- dfr.get.timeout(config.replyTimeout).guarantee(responses.remove(id).void)
        } yield r

      override def terminate: F[Unit] = fib.cancel *> closeSignal.set(true)
    }

    for {
      blocker <- Blocker[F]
      group <- SocketGroup(blocker)
      to = new InetSocketAddress(config.host, config.port)
      _ <- Resource.liftF(L.debug(s"open connection to $to"))
      client <- group.client(to)
      arango <- Resource.make(impl(client))(_.terminate).evalTap(_.login(config.username, config.password))
    } yield arango
  }

  /*
val loop: F[Unit] = {

val policy = RetryPolicies.limitRetries[F](5)

def onError(err: Throwable, details: RetryDetails): F[Unit] =
  details match {
    case WillDelayAndRetry(nextDelay: FiniteDuration, retriesSoFar: Int, _) =>
      stateSignal.set(ConnectionState.Connecting)

    case GivingUp(_, _) =>
      stateSignal.set(ConnectionState.Error) *> L.error(err)("giving up")
  }

retryingOnAllErrors(policy, onError) {
  mkSocket.use(_.pump)
} >> stateSignal.get.flatMap {
  case ConnectionState.Disconnected => closeSignal.set(false) >> loop
  case _                            => C.unit
}
}
 */

}
