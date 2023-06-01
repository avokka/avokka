package avokka.arangodb
package fs2

import _root_.fs2.Pipe
import _root_.fs2.concurrent.SignallingRef
import _root_.fs2.io.net._
import protocol.ArangoClient
import avokka.velocystream._
import cats.effect._
import cats.effect.syntax.spawn._
import cats.effect.syntax.temporal._
import cats.effect.syntax.monadCancel._
import cats.effect.syntax.resource._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.typelevel.log4cats.Logger
import scodec.bits.ByteVector
import com.comcast.ip4s._

trait Arango[F[_]] extends ArangoClient[F] {
   def terminate: F[Unit]
}

object Arango {

  def apply[F[_]: Network: Temporal](
    config: ArangoConfiguration,
  )(implicit C: Concurrent[F], L: Logger[F]): Resource[F, Arango[F]] = {

    def in(responses: FMap[F, Long, Deferred[F, ByteVector]]): Pipe[F, VStreamMessage, Unit] = _.evalMapChunk { msg =>
      responses.remove(msg.id).flatMap {
        case Some(deferred) => deferred.complete(msg.data).void
        case None           => C.raiseError[Unit](new Exception(s"unknown message id ${msg.id}"))
      }
    }

    def impl(client: Socket[F]): F[Arango[F]] = for {
      counter <- Ref.of(0L)
      responses <- FMap[F, Long, Deferred[F, ByteVector]]
      stateSignal <- SignallingRef[F, ConnectionState](ConnectionState.Disconnected)
      socket <- VChunkSocket(config, client, stateSignal, in(responses))
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

      override def terminate: F[Unit] = fib.cancel
    }

    val addr = C.fromOption(
      (Host.fromString(config.host), Port.fromInt(config.port)).tupled.map {
        case (host, port) => new SocketAddress(host, port)
      },
      new IllegalArgumentException(s"invalid address ${config.host}:${config.port}")
    )

    for {
      to <- addr.toResource
      _ <- L.debug(s"open connection to $to").toResource
      client <- Network[F].client(to)
      arango <- Resource.make(impl(client))(_.terminate).evalTap(_.login(config.username, config.password))
    } yield arango
  }

}
