package avokka.arangodb.protocol

import avokka.arangodb.ResponseError

sealed trait ArangoError extends Exception with Product with Serializable

object ArangoError {

  sealed trait WithCode {
    def code: Int
  }
  final case class Head(header: ArangoResponse.Header)
      extends Exception(s"header error ${header.responseCode}")
      with ArangoError
      with WithCode {
    override def code: Int = header.responseCode
  }

  sealed trait WithNum extends WithCode {
    def num: Long
  }
  final case class Resp(header: ArangoResponse.Header, error: ResponseError)
      extends Exception(error.errorMessage)
      with ArangoError
      with WithNum {
    override def code: Int = header.responseCode
    override def num: Long = error.errorNum
  }

//  final case class Runtime(message: String) extends Exception(message) with ArangoError
}
