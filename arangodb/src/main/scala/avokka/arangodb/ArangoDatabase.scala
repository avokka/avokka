package avokka.arangodb

import api._
import avokka.velocypack._
import protocol._
import types._

trait ArangoDatabase[F[_]] {
  def name: DatabaseName

  def collection(name: CollectionName): ArangoCollection[F]
  def document(handle: DocumentHandle): ArangoDocument[F]

  /**
    * @param users Has to be an array of user objects to initially create for the new database. User information will not be changed for users that already exist. If *users* is not specified or does not contain any users, a default user *root* will be created with an empty string password. This ensures that the new database will be accessible after it is created. Each user object can contain the following attributes:
    */
  def create(users: DatabaseCreate.User*): F[ArangoResponse[DatabaseCreate]]

  def info(): F[ArangoResponse[DatabaseInfo]]
  def drop(): F[ArangoResponse[DatabaseDrop]]

  def collections(excludeSystem: Boolean = false): F[ArangoResponse[api.CollectionList]]

  /**
    * @param query contains the query string to be executed
    * @param bindVars key/value pairs representing the bind parameters.
    * @param batchSize maximum number of result documents to be transferred from the server to the client in one roundtrip. If this attribute is not set, a server-controlled default value will be used. A *batchSize* value of *0* is disallowed.
    * @param cache flag to determine whether the AQL query results cache shall be used. If set to *false*, then any query cache lookup will be skipped for the query. If set to *true*, it will lead to the query cache being checked for the query if the query cache mode is either *on* or *demand*.
    * @param count indicates whether the number of documents in the result set should be returned in the \"count\" attribute of the result. Calculating the \"count\" attribute might have a performance impact for some queries in the future so this option is turned off by default, and \"count\" is only returned when requested.
    * @param memoryLimit the maximum number of memory (measured in bytes) that the query is allowed to use. If set, then the query will fail with error \"resource limit exceeded\" in case it allocates too much memory. A value of *0* indicates that there is no memory limit.
    * @param options
    * @param ttl The time-to-live for the cursor (in seconds). The cursor will be removed on the server automatically after the specified amount of time. This is useful to ensure garbage collection of cursors that are not fully fetched by clients. If not set, a server-defined value will be used (default: 30 seconds).
    */
  /*
  def query[V: VPackEncoder, T: VPackDecoder](
                                 query: String,
                                 bindVars: V,
                                 batchSize: Option[Long] = None,
                                 cache: Option[Boolean] = None,
                                 count: Option[Boolean] = None,
                                 memoryLimit: Option[Long] = None,
                                 options: Option[Cursor.Options] = None,
                                 ttl: Option[Long] = None,
                               ): F[ArangoResponse[Cursor.Response[T]]]
   */
}

object ArangoDatabase {
  def apply[F[_]: ArangoProtocol](_name: DatabaseName): ArangoDatabase[F] = new ArangoDatabase[F] {
    override def name: DatabaseName = _name

    override def collection(name: CollectionName): ArangoCollection[F] = ArangoCollection(this, name)
    override def document(handle: DocumentHandle): ArangoDocument[F] = ArangoDocument(this, handle)

    override def collections(excludeSystem: Boolean): F[ArangoResponse[CollectionList]] =
      ArangoProtocol[F].execute(ArangoRequest.GET(
        name,
        "/_api/collection",
        Map(
          "excludeSystem" -> excludeSystem.toString
        )
      ))

    override def create(users: DatabaseCreate.User*): F[ArangoResponse[DatabaseCreate]] = {
      ArangoProtocol[F].execute(
        ArangoRequest.POST(
          DatabaseName.system,
          "/_api/database"
        ).body(VObject(
          "name" -> name.toVPack,
          "users" -> users.toVPack
        ))
      )
    }

    override def info(): F[ArangoResponse[DatabaseInfo]] = ArangoProtocol[F].execute(
      ArangoRequest.GET(
        name,
        "/_api/database/current"
      )
    )

    override def drop(): F[ArangoResponse[DatabaseDrop]] = ArangoProtocol[F].execute(
      ArangoRequest.DELETE(
        DatabaseName.system,
        s"/_api/database/$name"
      )
    )
  }
}