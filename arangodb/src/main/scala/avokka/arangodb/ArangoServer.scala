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

  /**
    * current log level settings
    * @return map of log topic to log level
    */
  def logLevel(): F[ArangoResponse[AdminLog.Levels]]

  /**
    * modifies the current log level settings
    */
  def logLevel(levels: AdminLog.Levels): F[ArangoResponse[AdminLog.Levels]]
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

    override def logLevel(): F[ArangoResponse[AdminLog.Levels]] =
      GET(DatabaseName.system, "/_admin/log/level").execute

    override def logLevel(levels: AdminLog.Levels): F[ArangoResponse[AdminLog.Levels]] =
      PUT(DatabaseName.system, "/_admin/log/level").body(levels).execute
  }
}