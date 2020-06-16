package avokka

import cats.effect.Concurrent
import cats.effect.syntax.concurrent._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.apply._
import fs2.concurrent.{Queue, SignallingRef}
import fs2.io.tcp.Socket
import fs2.{Chunk, Pipe, Stream}
import io.chrisdavenport.log4cats.Logger
import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs.{bytes, fixedSizeBytes, uint32L}
import scodec.stream.{StreamDecoder, StreamEncoder}
import shapeless.{HNil, ::}

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
      in: Pipe[F, VChunk, Unit],
    )(implicit C: Concurrent[F], L: Logger[F]): F[VChunkSocket[F]] = {

    for {
      requests <- Queue.bounded[F, VChunk](requestsQueueSize)
    } yield {

      val chunks: Stream[F, VChunk] = requests.dequeue.evalMapChunk { vc =>
        // take a chunk of length and re-enqueue the remainder
        vc.take(config.chunkLength, requests.enqueue1)
      }

      val outgoing: Stream[F, Unit] = chunks
        .evalTap(msg => L.debug(s"${Console.BLUE}SEND${Console.RESET}: ${msg}"))
        .through(VChunk.streamEncoder.toPipeByte)
        .cons(handshake)
        .through(socket.writes())

      val incoming: Stream[F, Unit] = socket
        .reads(config.readBufferSize)
        .through(VChunk.streamDecoder.toPipeByte)
        .evalTap(msg => L.debug(s"${Console.BLUE_B}${Console.WHITE}RECV${Console.RESET}: ${msg}"))
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

  /*
  val chunkSplitter: Pipe[F, (ChunkHeader, ByteVector), (ChunkHeader, ByteVector)] =
    _.evalMapChunk {
      case (header, data) =>
        val (chunk, tail) = data.splitAt(config.chunkLength)
        requests.enqueue1(header.next -> tail).whenA(tail.nonEmpty).as(header -> chunk)
    }

  val dequeueChunk: F[(ChunkHeader, ByteVector)] =
    for {
      (header, data) <- requests.dequeue1
      (chunk, tail) = data.splitAt(config.chunkLength)
      _ <- requests.enqueue1(header.next -> tail).whenA(tail.nonEmpty)
    } yield header -> chunk
*/
  /*
val chunkDecoder: Decoder[(ChunkHeader, ByteVector)] = for {
len  <- uint32L
header <- ChunkHeader.codec
data <- fixedSizeBytes(len - 4 - 4 - 8 - 8, bytes)
} yield header -> data

val chunkEncoder: Encoder[(ChunkHeader, ByteVector)] = Encoder {
case (h: ChunkHeader, b: ByteVector) => for {
  hb <- ChunkHeader.codec.encode(h)
} yield hb ++ b.bits
}
*/
}
