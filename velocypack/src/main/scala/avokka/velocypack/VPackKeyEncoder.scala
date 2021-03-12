package avokka.velocypack

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot find a velocypack key encoder for ${T}")
trait VPackKeyEncoder[T] {
  def encode(t: T): String
}

object VPackKeyEncoder {
  @inline def apply[T](implicit e: VPackKeyEncoder[T]): VPackKeyEncoder[T] = e

  implicit val vpackKeyEncoderString: VPackKeyEncoder[String] = identity

  implicit val vpackKeyEncoderSymbol: VPackKeyEncoder[Symbol] = _.name
}
