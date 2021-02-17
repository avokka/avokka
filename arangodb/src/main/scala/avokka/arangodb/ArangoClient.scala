package avokka.arangodb

import avokka.arangodb.api.{DatabaseList, Version}
import protocol._
import types._

trait ArangoClient[F[_]] {
  def database(name: DatabaseName): ArangoDatabase[F]

  def databases: F[ArangoResponse[api.DatabaseList]]

  def version(details: Boolean = false): F[ArangoResponse[api.Version]]
}

object ArangoClient {
  def apply[F[_]: ArangoProtocol]: ArangoClient[F] = new ArangoClient[F] {

    override def database(name: DatabaseName): ArangoDatabase[F] = ArangoDatabase(name)

    override def version(details: Boolean): F[ArangoResponse[Version]] = ArangoProtocol[F].execute(ArangoRequest.GET(
      DatabaseName.system,
      "/_api/version",
      parameters = Map(
        "details" -> details.toString
      )
    ))

    override def databases: F[ArangoResponse[DatabaseList]] = ArangoProtocol[F].execute(ArangoRequest.GET(
      DatabaseName.system,
      "/_api/database/user",
    ))
  }
}