package avokka.arangodb

import models._
import admin.AdminLog
import protocol._
import types._
import cats.Functor
import cats.syntax.functor._

/**
  * ArangoDB server API
  *
  * @see https://www.arangodb.com/docs/stable/http/miscellaneous-functions.html
  * @tparam F effect
  */
trait ArangoServer[F[_]] {

  /**
    * list databases
    *
    * @return database names
    */
  def databases(): F[ArangoResponse[Vector[DatabaseName]]]

  /**
    * server version
    *
    * @param details include details
    * @return version
    */
  def version(details: Boolean = false): F[ArangoResponse[Version]]

  /**
    * server engine
    *
    * @return engine
    */
  def engine(): F[ArangoResponse[Engine]]

  /**
    * server role
    *
    * @return role
    */
  def role(): F[ArangoResponse[ServerRole]]

  /**
    * current log level settings
    *
    * @return map of log topic to log level
    */
  def logLevel(): F[ArangoResponse[AdminLog.Levels]]

  /**
    * modifies the current log level settings
    */
  def logLevel(levels: AdminLog.Levels): F[ArangoResponse[AdminLog.Levels]]
}

object ArangoServer {
  def apply[F[_]: ArangoClient : Functor]: ArangoServer[F] = new ArangoServer[F] {

    override def version(details: Boolean): F[ArangoResponse[Version]] =
      GET(DatabaseName.system, "/_api/version", parameters = Map("details" -> details.toString)).execute

    override def engine(): F[ArangoResponse[Engine]] =
      GET(DatabaseName.system, "/_api/engine").execute

    override def databases(): F[ArangoResponse[Vector[DatabaseName]]] =
      GET(DatabaseName.system, "/_api/database/user").execute[F, Result[Vector[DatabaseName]]].map(_.result)

    override def role(): F[ArangoResponse[ServerRole]] =
      GET(DatabaseName.system, "/_admin/server/role").execute

    override def logLevel(): F[ArangoResponse[AdminLog.Levels]] =
      GET(DatabaseName.system, "/_admin/log/level").execute

    override def logLevel(levels: AdminLog.Levels): F[ArangoResponse[AdminLog.Levels]] =
      PUT(DatabaseName.system, "/_admin/log/level").body(levels).execute
  }
}