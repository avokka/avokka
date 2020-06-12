package avokka

import java.net.InetSocketAddress

import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.syntax.concurrent._
import cats.effect.{Blocker, Concurrent, ContextShift, IO, Resource, Timer}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Pipe
import fs2.concurrent.SignallingRef
import fs2.io.tcp.SocketGroup
import io.chrisdavenport.log4cats.Logger
import pureconfig.ConfigSource
import retry.RetryDetails.{GivingUp, WillDelayAndRetry}
import retry.{RetryDetails, RetryPolicies, retryingOnAllErrors}
import scodec.bits.ByteVector

import scala.concurrent.duration.FiniteDuration

trait Transport[F[_]] {
  def execute(data: ByteVector): F[ByteVector]
  def terminate: F[Unit]
}

object Transport {

  def apply[F[_]: ContextShift: Timer](
      config: Configuration,
  )(implicit C: Concurrent[F], L: Logger[F]): F[Resource[F, Transport[F]]] = {
    for {
      counter <- Ref.of(0L)
      responses <- FMap[F, Long, Deferred[F, ByteVector]]
      stateSignal <- SignallingRef[F, ConnectionState](ConnectionState.Disconnected)
      closeSignal <- SignallingRef[F, Boolean](false)
    } yield {

      val in: Pipe[F, (ChunkHeader, ByteVector), Unit] = _.debug().evalMapChunk {
        case (header, vector) =>
          responses.remove(header.message).flatMap {
            case Some(value) => value.complete(vector)
            case None        => C.raiseError[Unit](new Exception("unknown message id"))
          }
      }.void

      val mkSocket: Resource[F, ChunkSocket[F]] = for {
        blocker <- Blocker[F]
        group <- SocketGroup(blocker)
        client <- group.client(new InetSocketAddress(config.host, config.port))
        socket <- Resource.liftF(ChunkSocket(config, client, stateSignal, closeSignal, in))
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
                _ <- L.debug(s"prepare message id = $id")
                dfr <- Deferred[F, ByteVector]
                _ <- responses.update(id, dfr)
                _ <- socket.send(id, data)
                r <- dfr.get
              } yield r

            override def terminate: F[Unit] = fib.cancel *> closeSignal.set(true)
          }
        }

      // socket.pump.background.as(transport)
      }
    }
  }

}
