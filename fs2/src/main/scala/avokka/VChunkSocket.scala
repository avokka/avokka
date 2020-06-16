package avokka

import cats.effect.Concurrent
import cats.effect.syntax.concurrent._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.concurrent.{Queue, SignallingRef}
import fs2.io.tcp.Socket
import fs2.{Chunk, Pipe, Stream}
import io.chrisdavenport.log4cats.Logger
import scodec.bits.ByteVector

trait VChunkSocket[F[_]] {
  def send(message: Long, data: ByteVector): F[Unit]
  def pump: F[Unit]
}

object VChunkSocket {

  val handshake: Chunk[Byte] = Chunk.bytes("VST/1.1\r\n\r\n".getBytes)

  val requestsQueueSize: Int = 128

  def apply[F[_]](
      config: Configuration,
      socket: Socket[F],
      stateSignal: SignallingRef[F, ConnectionState],
      closeSignal: SignallingRef[F, Boolean],
      in: Pipe[F, (Long, ByteVector), Unit],
    )(implicit C: Concurrent[F], L: Logger[F]): F[VChunkSocket[F]] = {

    for {
      requests <- Queue.bounded[F, VChunk](requestsQueueSize)
      history  <- VChunkHistory[F]
    } yield {

      val chunks: Stream[F, VChunk] = requests.dequeue.evalMapChunk { vc =>
        // take a chunk of length and re-enqueue the remainder
        vc.take(config.chunkLength, requests.enqueue1)
      }

      val outgoing: Stream[F, Unit] = chunks
        .evalTap(msg => L.debug(s"${Console.BLUE}SEND${Console.RESET}: $msg"))
        .through(VChunk.streamEncoder.toPipeByte)
        .cons(handshake)
        .through(socket.writes())

      val incoming: Stream[F, Unit] = socket
        .reads(config.readBufferSize)
        .through(VChunk.streamDecoder.toPipeByte)
        .evalTap(msg => L.debug(s"${Console.BLUE_B}${Console.WHITE}RECV${Console.RESET}: $msg"))
        .evalMap(ch => history.push(ch)).unNone
        .through(in)

      val data = incoming.merge(outgoing)
        .onFinalize(
          L.debug("CLOSE") *> stateSignal.set(ConnectionState.Disconnected)
        )
        .compile
        .drain

      val close: F[Unit] = closeSignal.discrete
        .evalMap(if (_) socket.close else C.unit)
        .compile
        .drain

      new VChunkSocket[F] {
        override def send(message: Long, data: ByteVector): F[Unit] = {
          requests.enqueue1(VChunk.message(message, data, config.chunkLength))
        }

        override val pump: F[Unit] =
          for {
            _ <- stateSignal.set(ConnectionState.Connected)
            _ <- data.race(close)
          } yield ()
      }
    }
  }

}
