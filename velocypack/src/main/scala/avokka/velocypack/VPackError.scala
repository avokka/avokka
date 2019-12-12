package avokka.velocypack

import scodec.Err

trait VPackError extends Throwable {

}

object VPackError {

  case object Overflow extends VPackError

  case object WrongType extends VPackError

  case class Conversion(from: Throwable) extends VPackError

  case class Codec(err: Err) extends VPackError {
    override def toString: String = err.toString()
  }

}
