package avokka.arangodb

import avokka.velocypack._

case class ResponseError
(
  code: Long,
  error: Boolean,
  errorMessage: String,
  errorNum: Long,
) extends VPackError {
  override def getMessage: String = errorMessage
}

object ResponseError {
  implicit val decoder: VPackDecoder[ResponseError] = VPackRecord[ResponseError].decoder
}