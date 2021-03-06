package avokka.arangodb
package protocol

import api.Result
import avokka.velocypack._
import cats.Show
import cats.syntax.show._

final case class ArangoResponse[T]
(
  header: ArangoResponse.Header,
  body: T
)

object ArangoResponse {

  final case class Header
  (
    version: Int,
    `type`: MessageType,
    responseCode: Int,
    meta: Map[String, String] = Map.empty
  )

  object Header {
    implicit val decoder: VPackDecoder[Header] = VPackGeneric[Header].decoder

    implicit val show: Show[Header] = { h =>
      show"${h.`type`}(v${h.version},code=${h.responseCode},meta=${h.meta})"
    }
  }

  implicit final class ArangoResponseResultOps[T](private val r: ArangoResponse[Result[T]]) {
    // extract result
    def result: ArangoResponse[T] = r.copy(body = r.body.result)
  }

}
