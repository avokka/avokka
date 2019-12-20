package avokka.velocypack

import scodec.Err

trait VPackError extends Throwable {}

object VPackError {

  case object Overflow extends VPackError

  case class WrongType(v: VPack) extends VPackError

  case class Conversion(from: Throwable) extends VPackError

  case class Codec(err: Err) extends VPackError {
    override def toString: String = err.toString()
  }

  case object NotEnoughElements extends VPackError

  case class ObjectFieldAbsent(name: String) extends VPackError

  case class IllegalValue(detailMessage: String) extends VPackError
}
