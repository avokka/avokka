package avokka

import cats.effect.Concurrent
import cats.effect.syntax.concurrent._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.concurrent.{Queue, SignallingRef}
import fs2.io.tcp.Socket
import fs2.{Chunk, Pipe, Stream}
import io.chrisdavenport.log4cats.Logger
import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs.{bytes, fixedSizeBytes, uint32L}
import scodec.stream.{StreamDecoder, StreamEncoder}

trait ChunkSocket[F[_]] {
  def send(message: Long, data: ByteVector): F[Unit]
  def pump: F[Unit]
}

object ChunkSocket {

  val handshake: Chunk[Byte] = Chunk.bytes("VST/1.1\r\n\r\n".getBytes)

  // 4 chunk length + 4 chunkx + 8 message id + 8 message length
  val chunkHeaderOffset: Long = 24

  val chunkCodec: Codec[(ChunkHeader, ByteVector)] = uint32L.consume { l =>
    ChunkHeader.codec ~ fixedSizeBytes(l - chunkHeaderOffset, bytes)
  } { _._2.size + chunkHeaderOffset }

  val chunkStreamDecoder: StreamDecoder[(ChunkHeader, ByteVector)] = StreamDecoder.many(chunkCodec)
  val chunkStreamEncoder: StreamEncoder[(ChunkHeader, ByteVector)] = StreamEncoder.many(chunkCodec)

  def apply[F[_]](
      config: Configuration,
      socket: Socket[F],
      stateSignal: SignallingRef[F, ConnectionState],
      closeSignal: SignallingRef[F, Boolean],
      in: Pipe[F, (ChunkHeader, ByteVector), Unit],
    )(implicit C: Concurrent[F], L: Logger[F]): F[ChunkSocket[F]] = {

    for {
      requests <- Queue.unbounded[F, (ChunkHeader, ByteVector)]
    } yield {

      val chunks: Stream[F, (ChunkHeader, ByteVector)] = requests.dequeue.evalMapChunk {
          case (header, data) =>
            val (chunk, tail) = data.splitAt(config.chunkLength)
            requests.enqueue1(header.next -> tail).whenA(tail.nonEmpty).as(header -> chunk)
        }

      val outgoing: F[Unit] = chunks
        .evalTap(msg => L.debug(s"SEND: ${msg._1} / ${msg._2}"))
        .through(chunkStreamEncoder.toPipeByte)
        .cons(handshake)
        .through(socket.writes())
        .onFinalize(stateSignal.set(ConnectionState.Disconnected))
        .compile
        .drain

      val incoming: F[Unit] = socket
        .reads(config.readBufferSize)
        .through(chunkStreamDecoder.toPipeByte)
        .evalTap(msg => L.debug(s"RECV: ${msg._1} / ${msg._2}"))
        .through(in)
        .onFinalize(stateSignal.set(ConnectionState.Disconnected))
        .compile
        .drain

      val close: F[Unit] = closeSignal.discrete
        .evalMap(if (_) socket.close else C.unit)
        .compile
        .drain

      new ChunkSocket[F] {
        override def send(message: Long, data: ByteVector): F[Unit] = {
          val chunksCount = (data.size.toDouble / config.chunkLength).ceil.toLong
          val header = ChunkHeader(ChunkX(first = true, chunksCount), message, data.size)
          requests.enqueue1(header -> data)
        }

        override val pump: F[Unit] =
          for {
            _ <- stateSignal.set(ConnectionState.Connected)
            _ <- outgoing.race(incoming).race(close)
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
