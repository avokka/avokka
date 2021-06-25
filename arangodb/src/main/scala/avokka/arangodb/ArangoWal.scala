package avokka.arangodb

import avokka.arangodb.models.Wal
import avokka.arangodb.protocol.{ArangoClient, ArangoResponse}
import avokka.arangodb.types.DatabaseName
import cats.Functor

trait ArangoWal[F[_]] {

  /**
    * Tail recent server operations
    *
    * @param global  Whether operations for all databases should be included. When set to false only the operations for the current database are included. The value true is only valid on the _system database. The default is false.
    * @param from Exclusive lower bound tick value for results. On successive calls to this API you should set this to the value returned with the x-arango-replication-lastincluded header (Unless that header contains 0).
    * @param to  Inclusive upper bound tick value for results.
    * @param lastScanned Should be set to the value of the x-arango-replication-lastscanned header or alternatively 0 on first try. This allows the rocksdb engine to break up large transactions over multiple responses.
    * @param chunkSize Approximate maximum size of the returned result.
    * @param syncerId Id of the client used to tail results. The server will use this to keep operations until the client has fetched them. Must be a positive integer. Note this or serverId is required to have a chance at fetching reading all operations with the rocksdb storage engine.
    * @param serverId Id of the client machine. If syncerId is unset, the server will use this to keep operations until the client has fetched them. Must be a positive integer. Note this or syncerId is required to have a chance at fetching reading all operations with the rocksdb storage engine.
    * @param clientInfo Short description of the client, used for informative purposes only.
    * @return data from the server’s write-ahead log (also named replication log)
    */
  def tail(
      global: Boolean = false,
      from: Option[String] = None,
      to: Option[String] = None,
      lastScanned: Option[String] = None,
      chunkSize: Option[Int] = None,
      syncerId: Option[Int] = None,
      serverId: Option[Int] = None,
      clientInfo: Option[String] = None
  ): F[ArangoResponse[Vector[Wal]]]
}

object ArangoWal {
  def apply[F[_]: ArangoClient: Functor](
      database: DatabaseName,
  ): ArangoWal[F] = new ArangoWal[F] {
    override def tail(
        global: Boolean = false,
        from: Option[String] = None,
        to: Option[String] = None,
        lastScanned: Option[String] = None,
        chunkSize: Option[Int] = None,
        syncerId: Option[Int] = None,
        serverId: Option[Int] = None,
        clientInfo: Option[String] = None
    ): F[ArangoResponse[Vector[Wal]]] =
      GET(
        database,
        "/_api/wal/tail",
        parameters = Map(
          "global" -> Some(global.toString),
          "from" -> from,
          "to" -> to,
          "lastScanned" -> lastScanned,
          "chunkSize" -> chunkSize.map(_.toString),
          "syncerId" -> syncerId.map(_.toString),
          "serverId" -> serverId.map(_.toString),
          "clientInfo" -> clientInfo
        ).collectDefined
      ).executeSequence
  }
}
