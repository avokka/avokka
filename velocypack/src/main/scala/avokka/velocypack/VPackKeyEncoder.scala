package avokka.velocypack

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot find a velocypack key encoder for ${T}")
trait VPackKeyEncoder[T] { self =>
  def encode(t: T): String

  def contramap[U](f: U => T): VPackKeyEncoder[U] = (u: U) => self.encode(f(u))
}

object VPackKeyEncoder {
  @inline def apply[T](implicit e: VPackKeyEncoder[T]): VPackKeyEncoder[T] = e

  implicit val vpackKeyEncoderString: VPackKeyEncoder[String] = identity(_)

  implicit val vpackKeyEncoderSymbol: VPackKeyEncoder[Symbol] = _.name
}
