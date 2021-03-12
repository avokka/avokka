package avokka.arangodb

import api._
import admin.AdminLog
import protocol._
import types._

import avokka.velocypack.enumeratum._

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

  def role(): F[ArangoResponse[ServerRole]]

  def logLevel(): F[ArangoResponse[Map[String, AdminLog.Level]]]
}

object ArangoServer {
  def apply[F[_]: ArangoClient]: ArangoServer[F] = new ArangoServer[F] {

    override def version(details: Boolean): F[ArangoResponse[Version]] =
      GET(DatabaseName.system, "/_api/version", parameters = Map("details" -> details.toString)).execute

    override def engine(): F[ArangoResponse[Engine]] =
      GET(DatabaseName.system, "/_api/engine").execute

    override def databases(): F[ArangoResponse[DatabaseList]] =
      GET(DatabaseName.system, "/_api/database/user").execute

    override def role(): F[ArangoResponse[ServerRole]] =
      GET(DatabaseName.system, "/_admin/server/role").execute

    override def logLevel(): F[ArangoResponse[Map[String, AdminLog.Level]]] =
      GET(DatabaseName.system, "/_admin/log/level").execute

  }
}