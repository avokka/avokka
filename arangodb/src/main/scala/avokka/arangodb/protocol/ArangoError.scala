package avokka.arangodb
package protocol

sealed trait ArangoError extends RuntimeException with Product with Serializable

object ArangoError {

  final case class Header(header: ArangoResponse.Header)
      extends RuntimeException("header error " + header.responseCode)
      with ArangoError

  final case class Response(header: ArangoResponse.Header, error: ResponseError)
      extends RuntimeException(error.errorMessage)
      with ArangoError
}
