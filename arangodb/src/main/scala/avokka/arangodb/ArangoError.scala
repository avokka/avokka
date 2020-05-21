package avokka.arangodb

import avokka.velocypack.VPackError

sealed trait ArangoError extends Throwable {
  def message: String
}

object ArangoError {
  final case class VPack(error: VPackError) extends ArangoError {
    override def message: String = error.message
  }
  trait WithCode {
    def code: Int
  }
  final case class Head(header: ArangoResponse.Header) extends ArangoError with WithCode {
    override def message: String = header.responseCode.toString
    override def code: Int = header.responseCode
  }
  trait WithNum extends WithCode {
    def num: Long
  }
  final case class Resp(header: ArangoResponse.Header, error: ResponseError) extends ArangoError with WithNum {
    override def message: String = error.errorMessage
    override def code: Int = header.responseCode
    override def num: Long = error.errorNum
  }
}
