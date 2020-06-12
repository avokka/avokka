import cats.ApplicativeError
import cats.syntax.either._
import scodec.{Attempt, DecodeResult, Decoder, Encoder, Err}
import scodec.bits.BitVector

package object avokka {
  final case class CodecException(err: Err) extends Exception(err.messageWithContext)

  private def attemptEither[T](attempt: Attempt[T]): Either[Throwable, T] = attempt.toEither.leftMap(CodecException)

  implicit final class ApplicativeDecoderOps[T](private val decoder: Decoder[T]) {
    def decodeF[F[_]](bits: BitVector)(implicit A: ApplicativeError[F, Throwable]): F[DecodeResult[T]] = A.fromEither(attemptEither(decoder.decode(bits)))
  }

  implicit final class ApplicativeEncoderOps[T](private val encoder: Encoder[T]) {
    def encodeF[F[_]](value: T)(implicit A: ApplicativeError[F, Throwable]): F[BitVector] = A.fromEither(attemptEither(encoder.encode(value)))
  }
}
