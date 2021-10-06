package avokka.arangodb
package protocol

import models.Result
import avokka.velocypack._
import cats.{Functor, Show}
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

  implicit val functor: Functor[ArangoResponse] = new Functor[ArangoResponse] {
    override def map[A, B](fa: ArangoResponse[A])(f: A => B): ArangoResponse[B] = fa.copy(body = f(fa.body))
  }

  final case class Error
  (
    code: Long,
    error: Boolean,
    errorNum: Long,
    errorMessage: String = "",
  )

  object Error {
    implicit val decoder: VPackDecoder[Error] = VPackDecoder.gen
  }
}
