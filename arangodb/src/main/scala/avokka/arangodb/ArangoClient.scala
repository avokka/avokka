package avokka.arangodb

import api._
import cats.Functor
import protocol._
import types._

trait ArangoClient[F[_]] {
  def database(name: DatabaseName): ArangoDatabase[F]
  // lazy val _system = database(DatabaseName.system)
  // lazy val db = database(configuration.database)

  def databases(): F[ArangoResponse[DatabaseList]]

  def version(details: Boolean = false): F[ArangoResponse[Version]]
  def engine(): F[ArangoResponse[Engine]]
}

object ArangoClient {
  def apply[F[_]: ArangoProtocol: Functor]: ArangoClient[F] = new ArangoClient[F] {

    override def database(name: DatabaseName): ArangoDatabase[F] = ArangoDatabase(name)

    override def version(details: Boolean): F[ArangoResponse[Version]] = ArangoProtocol[F].execute(ArangoRequest.GET(
      DatabaseName.system,
      "/_api/version",
      parameters = Map(
        "details" -> details.toString
      )
    ))

    override def engine(): F[ArangoResponse[Engine]] = ArangoProtocol[F].execute(ArangoRequest.GET(
      DatabaseName.system,
      "/_api/engine",
    ))

    override def databases(): F[ArangoResponse[DatabaseList]] = ArangoProtocol[F].execute(ArangoRequest.GET(
      DatabaseName.system,
      "/_api/database/user",
    ))
  }
}