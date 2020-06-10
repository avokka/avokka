package avokka

import cats.effect.Concurrent
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.{Chunk, Pipe, Stream}
import fs2.concurrent.SignallingRef
import fs2.io.tcp.Socket
import io.chrisdavenport.log4cats.Logger
import scodec.Decoder
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.{bytes, fixedSizeBytes, uint32L}
import scodec.stream.StreamDecoder

trait Transport[F[_]] {
  def close: F[Unit]
}

object Transport {

  val chunkDecoder: Decoder[(ChunkHeader, ByteVector)] = for {
    len  <- uint32L
    header <- ChunkHeader.codec
    data <- fixedSizeBytes(len - 4 - 4 - 8 - 8, bytes)
  } yield header -> data

  val chunkStreamDecoder: StreamDecoder[(ChunkHeader, ByteVector)] = StreamDecoder.many(chunkDecoder)

  val readBufferSize = 256 * 1024

  def fromSocket[F[_]](socket: Socket[F])(
    implicit C: Concurrent[F], L: Logger[F]
  ): F[Transport[F]] = C.pure(
    new Transport[F] {
      override def close: F[Unit] = C.unit

      def write(header: ChunkHeader, chunk: ByteVector): F[Unit] = for {
        _      <- L.debug(s"write chunk header = $header")
        header <- ChunkHeader.codec.encodeA(header).map(_.bytes)
        len    <- uint32L.encodeA(4 + header.size + chunk.size).map(_.bytes)
        w      <- socket.write(Chunk.byteVector(len ++ header ++ chunk))
      } yield w

      def read(): Stream[F, (ChunkHeader, ByteVector)] = socket.reads(readBufferSize)
        .through(chunkStreamDecoder.toPipeByte)
        .evalTapChunk { chbv =>
          L.debug(s"received chunk header = ${chbv._1}")
        }

    }
  )

  def apply[F[_]](
                   in: Pipe[F, (ChunkHeader, ByteVector), Unit],
                   out: Stream[F, (ChunkHeader, ByteVector)],
              //     stateSignal: SignallingRef[F, ConnectionState],
                   closeSignal: SignallingRef[F, Boolean]
                 ): F[Transport[F]] = ???

}
