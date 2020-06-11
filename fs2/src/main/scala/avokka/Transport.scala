package avokka

import java.net.InetSocketAddress

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Timer}
import fs2.concurrent.{Queue, SignallingRef}
import fs2.io.tcp.{Socket, SocketGroup}
import fs2.{Chunk, Pipe, Stream}
import io.chrisdavenport.log4cats.Logger
import retry.RetryDetails.{GivingUp, WillDelayAndRetry}
import retry.{RetryDetails, RetryPolicies, retryingOnAllErrors}
import scodec.Codec
import scodec.bits.ByteVector
import scodec.stream.{StreamDecoder, StreamEncoder}
import scodec.codecs.{bytes, fixedSizeBytes, uint32L}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicative._
import cats.effect.syntax.concurrent._

import scala.concurrent.duration.FiniteDuration

trait Transport[F[_]] {
  def enqueueMessage(data: ByteVector): F[Unit]
}

object Transport {

  val handshake: Chunk[Byte] = Chunk.bytes("VST/1.1\r\n\r\n".getBytes)

  // 4 chunk length + 4 chunkx + 8 message id + 8 message length
  val chunkHeaderOffset = 24

  val chunkCodec: Codec[(ChunkHeader, ByteVector)] = uint32L.consume { l =>
    ChunkHeader.codec ~ fixedSizeBytes(l - chunkHeaderOffset, bytes)
  } { _._2.size + chunkHeaderOffset }

  val chunkStreamDecoder: StreamDecoder[(ChunkHeader, ByteVector)] = StreamDecoder.many(chunkCodec)
  val chunkStreamEncoder: StreamEncoder[(ChunkHeader, ByteVector)] = StreamEncoder.many(chunkCodec)

  def apply[F[_] : ContextShift: Timer](
      config: Configuration,
      in: Pipe[F, (ChunkHeader, ByteVector), Unit],
      counter: Ref[F, Long],
      requests: Queue[F, (ChunkHeader, ByteVector)],
  //    out: Stream[F, (ChunkHeader, ByteVector)],
      stateSignal: SignallingRef[F, ConnectionState],
      closeSignal: SignallingRef[F, Boolean],
  )(implicit C: Concurrent[F], L: Logger[F]): F[Transport[F]] = {

    val chunkSplitter: Pipe[F, (ChunkHeader, ByteVector), (ChunkHeader, ByteVector)] =
      _.evalMapChunk {
        case (header, data) =>
          val (chunk, tail) = data.splitAt(config.chunkLength)
          requests.enqueue1(header.next -> tail).whenA(tail.nonEmpty).as(header -> chunk)
      }

    /*
    val dequeueChunk: F[(ChunkHeader, ByteVector)] =
      for {
        (header, data) <- requests.dequeue1
        (chunk, tail) = data.splitAt(config.chunkLength)
        _ <- requests.enqueue1(header.next -> tail).whenA(tail.nonEmpty)
      } yield header -> chunk
*/

    def outgoing(socket: Socket[F]): F[Unit] = requests
        .dequeue
        .through(chunkSplitter)
        .through(chunkStreamEncoder.toPipeByte)
        .cons(handshake)
        .through(socket.writes())
        .onFinalize(stateSignal.set(ConnectionState.Disconnected))
        .compile
        .drain

    def incoming(socket: Socket[F]): F[Unit] = socket
        .reads(config.readBufferSize)
        .through(chunkStreamDecoder.toPipeByte)
        .through(in)
        .onFinalize(stateSignal.set(ConnectionState.Disconnected))
        .compile
        .drain

    def close(socket: Socket[F]) =
      closeSignal.discrete.evalMap(if (_) socket.close else C.unit).compile.drain

    def pump(socket: Socket[F]) =
      for {
        _ <- stateSignal.set(ConnectionState.Connected)
        _ <- outgoing(socket).race(incoming(socket)).race(close(socket))
      } yield ()

    val mkSocket: Resource[F, Socket[F]] = for {
      blocker <- Blocker[F]
      group <- SocketGroup(blocker)
      client <- group.client(new InetSocketAddress(config.host, config.port))
    } yield client

    def loop(): F[Unit] = {

      val policy = RetryPolicies.limitRetries[F](5)

      def onError(err: Throwable, details: RetryDetails): F[Unit] =
        details match {
          case WillDelayAndRetry(nextDelay: FiniteDuration, retriesSoFar: Int, _) =>
            stateSignal.set(ConnectionState.Connecting)

          case GivingUp(_, _) =>
            stateSignal.set(ConnectionState.Error) *> L.error(err)("giving up")
        }

      retryingOnAllErrors(policy, onError) {
        mkSocket.use(pump)
      } >> stateSignal.get.flatMap {
        case ConnectionState.Disconnected => closeSignal.set(false) >> loop()
        case _                            => C.unit
      }
    }

    loop().start.as(new Transport[F] {
      override def enqueueMessage(data: ByteVector): F[Unit] = {
        val chunksCount = (data.size.toDouble / config.chunkLength).ceil.toLong

        for {
          id <- counter.getAndUpdate(_ + 1)
          header = ChunkHeader(ChunkX(first = true, chunksCount), id, data.size)
          _ <- requests.enqueue1(header -> data)
        } yield ()
      }
    })
  }

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
