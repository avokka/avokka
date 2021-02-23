package avokka

import avokka.velocystream._
import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.syntax.bracket._
import cats.effect.syntax.concurrent._
import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Timer}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Pipe
import fs2.concurrent.SignallingRef
import fs2.io.tcp.SocketGroup
import org.typelevel.log4cats.Logger
import scodec.bits.ByteVector

import java.net.InetSocketAddress

trait Transport[F[_]] {
  def execute(data: ByteVector): F[ByteVector]
  def terminate: F[Unit]
}

object Transport {

  def apply[F[_]: ContextShift: Timer](
    config: VStreamConfiguration,
  )(implicit C: Concurrent[F], L: Logger[F]): F[Resource[F, Transport[F]]] = for {
      counter <- Ref.of(0L)
      responses <- FMap[F, Long, Deferred[F, ByteVector]]
      stateSignal <- SignallingRef[F, ConnectionState](ConnectionState.Disconnected)
      closeSignal <- SignallingRef[F, Boolean](false)
    } yield {

      val in: Pipe[F, VStreamMessage, Unit] = _.evalMapChunk { msg =>
          responses.remove(msg.id).flatMap {
            case Some(deferred) => deferred.complete(msg.data)
            case None           => C.raiseError[Unit](new Exception(s"unknown message id ${msg.id}"))
          }
      }

      val mkSocket: Resource[F, VChunkSocket[F]] = for {
          blocker <- Blocker[F]
          group <- SocketGroup(blocker)
          to = new InetSocketAddress(config.host, config.port)
          _ <- Resource.liftF(L.debug(s"open connection to $to"))
          client <- group.client(to)
          socket <- Resource.liftF(VChunkSocket(config, client, stateSignal, closeSignal, in))
        } yield socket

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

      mkSocket.evalMap { socket =>
        socket.pump.start.map { fib =>
          new Transport[F] {
            override def execute(data: ByteVector): F[ByteVector] =
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
        }

      // socket.pump.background.as(transport)
      }
    }

}
