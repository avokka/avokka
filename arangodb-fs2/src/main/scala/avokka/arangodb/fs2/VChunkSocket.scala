package avokka.arangodb.fs2

import avokka.velocystream._
import cats.effect.Concurrent
import cats.effect.std.Queue
import cats.effect.syntax.spawn._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.foldable._
import cats.syntax.show._
import fs2.concurrent.SignallingRef
import fs2.io.net.Socket
import fs2.{Chunk, Pipe, Stream}
import org.typelevel.log4cats.Logger
import scodec.stream.{StreamDecoder, StreamEncoder}

trait VChunkSocket[F[_]] {
  def send(message: VStreamMessage): F[Unit]
  def pump: F[Unit]
}

object VChunkSocket {

  val handshake: Chunk[Byte] = Chunk.array("VST/1.1\r\n\r\n".getBytes)

  val requestsQueueSize: Int = 128

  def apply[F[_]](
      config: VStreamConfiguration,
      socket: Socket[F],
      stateSignal: SignallingRef[F, ConnectionState],
      closeSignal: SignallingRef[F, Boolean],
      in: Pipe[F, VStreamMessage, Unit],
    )(implicit C: Concurrent[F], L: Logger[F]): F[VChunkSocket[F]] = {

    for {
      chunks    <- Queue.bounded[F, VStreamChunk](requestsQueueSize)
      assembler <- VChunkAssembler[F]
    } yield {

      val streamDecoder: Pipe[F, Byte, VStreamChunk] = StreamDecoder.many(VStreamChunk.codec).toPipeByte
      val streamEncoder: Pipe[F, VStreamChunk, Byte] = StreamEncoder.many(VStreamChunk.codec).toPipeByte

      val outgoing: Stream[F, Unit] = Stream.fromQueueUnterminated(chunks)
        .evalMapChunk { chunk =>
          // take a chunk of length and re-enqueue the remainder
          val (head, remainder) = chunk.split(config.chunkLength)
          remainder.traverse_(chunks.offer).as(head)
        }
        .evalTap(msg => L.trace(show"${Console.BLUE}SEND${Console.RESET} $msg"))
        .through(streamEncoder)
        .cons(handshake)
        .through(socket.writes)

      val incoming: Stream[F, Unit] = socket
        .reads
        // .reads(config.readBufferSize)
        .through(streamDecoder)
        .evalTap(msg => L.trace(show"${Console.BLUE_B}${Console.WHITE}RECV${Console.RESET} $msg"))
        .evalMap(ch => assembler.push(ch).value).unNone
        .through(in)

      val data = incoming.merge(outgoing)
        .onFinalize(
          L.debug("CLOSE") *> stateSignal.set(ConnectionState.Disconnected)
        )
        .compile
        .drain

      val close: F[Unit] = closeSignal.discrete
        //.evalMap(if (_) socket.close else C.unit)
        .compile
        .drain

      new VChunkSocket[F] {
        override def send(message: VStreamMessage): F[Unit] = {
          chunks.offer(message.firstChunk(config.chunkLength))
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
