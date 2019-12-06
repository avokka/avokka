package avokka.arangodb

import avokka.velocypack._
import avokka.velocypack.codecs.{VPackRecordCodec, VPackRecordDefaultsCodec}
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
  implicit val codec: Codec[ResponseError] = VPackRecordCodec[ResponseError].codec
}