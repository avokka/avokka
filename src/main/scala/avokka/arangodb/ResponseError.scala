package avokka.arangodb

import avokka.velocypack._
import scodec.Codec

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
  implicit val codec: Codec[ResponseError] = VPackRecord[ResponseError].codec
}