package avokka.velocypack

import cats.syntax.either._

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot find a velocypack key decoder for ${T}")
trait VPackKeyDecoder[T] {
  def decode(key: String): VPackResult[T]
}

object VPackKeyDecoder {
  @inline def apply[T](implicit e: VPackKeyDecoder[T]): VPackKeyDecoder[T] = e

  implicit val vpackKeyDecoderString: VPackKeyDecoder[String] = _.asRight

  implicit val vpackKeyDecoderSymbol: VPackKeyDecoder[Symbol] = Symbol(_).asRight
}
