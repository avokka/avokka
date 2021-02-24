package avokka.arangodb

import avokka.velocypack._

final case class ResponseError(
    code: Long,
    error: Boolean,
    errorNum: Long,
    errorMessage: String = "",
)

object ResponseError {
  implicit val decoder: VPackDecoder[ResponseError] = VPackDecoder.gen
}
