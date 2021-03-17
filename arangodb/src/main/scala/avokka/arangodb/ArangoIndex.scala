package avokka.arangodb

import models._
import avokka.arangodb.types.DatabaseName
import protocol._

trait ArangoIndex[F[_]] {
  def id: String
  def read(): F[ArangoResponse[Index]]
  def delete(): F[ArangoResponse[DeleteResult]]
}

object ArangoIndex {
  def apply[F[_]: ArangoClient](database: DatabaseName, _id: String): ArangoIndex[F] = new ArangoIndex[F] {

    override def id: String = _id

    private val api: String = "/_api/index/" + id

    override def read(): F[ArangoResponse[Index]] =
      GET(database, api).execute

    override def delete(): F[ArangoResponse[DeleteResult]] =
      DELETE(database, api).execute

  }
}
