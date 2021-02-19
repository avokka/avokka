package avokka.arangodb

import api._
import avokka.arangodb.types.DatabaseName
import protocol._

trait ArangoIndex[F[_]] {
  def id: String
  def read(): F[ArangoResponse[Index]]
  def delete(): F[ArangoResponse[DeleteResult]]
}

object ArangoIndex {
  def apply[F[_]: ArangoProtocol](database: DatabaseName, _id: String): ArangoIndex[F] = new ArangoIndex[F] {

    override def id: String = _id

    override def read(): F[ArangoResponse[Index]] = ArangoProtocol[F].execute(
      ArangoRequest.GET(
        database,
        s"/_api/index/$id"
      )
    )

    override def delete(): F[ArangoResponse[DeleteResult]] = ArangoProtocol[F].execute(
      ArangoRequest.DELETE(
        database,
        s"/_api/index/$id"
      )
    )
  }
}
