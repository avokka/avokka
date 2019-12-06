package avokka.velocypack

trait VPackError extends Throwable {

}

object VPackError {

  case class Codec(err: String) extends VPackError {
    override def toString: String = err
  }

}
