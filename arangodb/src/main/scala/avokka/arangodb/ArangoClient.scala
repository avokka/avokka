package avokka.arangodb

import api._
import cats.Functor
import protocol._
import types._

trait ArangoClient[F[_]] {
  /**
    * database api
    * @param name database name
    * @return database api
    */
  def database(name: DatabaseName): ArangoDatabase[F]

  /**
    * _system database api
    * @return database
    */
  def system: ArangoDatabase[F]

  /**
    * configured database api
    * @return database
    */
  def db: ArangoDatabase[F]

  /**
    * list databases
    * @return
    */
  def databases(): F[ArangoResponse[DatabaseList]]

  /**
    * server version
    * @param details include details
    * @return version
    */
  def version(details: Boolean = false): F[ArangoResponse[Version]]

  /**
    * server engine
    * @return engine
    */
  def engine(): F[ArangoResponse[Engine]]
}

object ArangoClient {
  def apply[F[_]: ArangoProtocol: Functor](configuration: ArangoConfiguration): ArangoClient[F] = new ArangoClient[F] {

    override def database(name: DatabaseName): ArangoDatabase[F] = ArangoDatabase(name)
    override def system: ArangoDatabase[F] = database(DatabaseName.system)
    override def db: ArangoDatabase[F] = database(configuration.database)

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