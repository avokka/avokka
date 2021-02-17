package avokka.arangodb
package api

import avokka.velocypack._
import types._

case class DatabaseInfo(
    result: DatabaseInfo.Result,
)

object DatabaseInfo {

  final case class Result(
      name: DatabaseName,
      id: String,
      path: String,
      isSystem: Boolean,
  )

  object Result {
    implicit val decoder: VPackDecoder[Result] = VPackDecoder.gen
  }

  implicit val decoder: VPackDecoder[DatabaseInfo] = VPackDecoder.gen

}
