package avokka.velocypack

trait VPackError extends Throwable {

}

case class VPackErrorCodec(err: String) extends VPackError {
  override def toString: String = err
}
