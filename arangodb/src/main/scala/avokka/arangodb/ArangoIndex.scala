package avokka.arangodb

import api._
import protocol._

trait ArangoIndex[F[_]] {
  def collection: ArangoCollection[F]
  def id: String

  def read(): F[ArangoResponse[Index]]

  def delete(): F[ArangoResponse[IndexDelete]]
}

object ArangoIndex {
  def apply[F[_]: ArangoProtocol](_collection: ArangoCollection[F], _id: String): ArangoIndex[F] = new ArangoIndex[F] {
    override def collection: ArangoCollection[F] = _collection
    override def id: String = _id

    override def read(): F[ArangoResponse[Index]] = ArangoProtocol[F].execute(
      ArangoRequest.GET(
        collection.database.name,
        s"/_api/index/$id"
      )
    )

    override def delete(): F[ArangoResponse[IndexDelete]] = ArangoProtocol[F].execute(
      ArangoRequest.DELETE(
        collection.database.name,
        s"/_api/index/$id"
      )
    )
  }
}
