package avokka.arangodb

import avokka.velocypack._

final case class ResponseError(
    code: Long,
    error: Boolean,
    errorMessage: String,
    errorNum: Long,
)

object ResponseError {
  implicit val decoder: VPackDecoder[ResponseError] = VPackRecord[ResponseError].decoder
}
