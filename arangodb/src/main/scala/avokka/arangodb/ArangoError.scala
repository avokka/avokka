package avokka.arangodb

import avokka.velocypack.VPackError

sealed trait ArangoError extends Exception with Product with Serializable

object ArangoError {
  final case class VPack(error: VPackError)
      extends Exception("velocypack error", error)
      with ArangoError

  trait WithCode {
    def code: Int
  }
  final case class Head(header: ArangoResponse.Header)
      extends Exception(s"header error ${header.responseCode}")
      with ArangoError
      with WithCode {
    override def code: Int = header.responseCode
  }

  trait WithNum extends WithCode {
    def num: Long
  }
  final case class Resp(header: ArangoResponse.Header, error: ResponseError)
      extends Exception(error.errorMessage)
      with ArangoError
      with WithNum {
    override def code: Int = header.responseCode
    override def num: Long = error.errorNum
  }
}
