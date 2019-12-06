package avokka.velocypack

import scodec.Err

trait VPackError extends Throwable {

}

object VPackError {

  case class Codec(err: Err) extends VPackError {
    override def toString: String = err.toString()
  }

}
