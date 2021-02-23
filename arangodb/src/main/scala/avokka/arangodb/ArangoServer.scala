package avokka.arangodb

import api._
import protocol._
import types._

trait ArangoServer[F[_]] {
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

object ArangoServer {
  def apply[F[_]: ArangoClient]: ArangoServer[F] = new ArangoServer[F] {

    override def version(details: Boolean): F[ArangoResponse[Version]] = ArangoClient[F].execute(ArangoRequest.GET(
      DatabaseName.system,
      "/_api/version",
      parameters = Map(
        "details" -> details.toString
      )
    ))

    override def engine(): F[ArangoResponse[Engine]] = ArangoClient[F].execute(ArangoRequest.GET(
      DatabaseName.system,
      "/_api/engine",
    ))

    override def databases(): F[ArangoResponse[DatabaseList]] = ArangoClient[F].execute(ArangoRequest.GET(
      DatabaseName.system,
      "/_api/database/user",
    ))
  }
}