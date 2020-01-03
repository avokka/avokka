package avokka.arangodb

import avokka.velocypack.VPackError

trait ArangoError {
  def message: String
}

object ArangoError {
  case class VPack(error: VPackError) extends ArangoError {
    override def message: String = error.message
  }
  case class Head(header: ArangoResponse.Header) extends ArangoError {
    override def message: String = header.responseCode.toString
  }
  case class Resp(header: ArangoResponse.Header, error: ResponseError) extends ArangoError {
    override def message: String = error.errorMessage
  }
}
