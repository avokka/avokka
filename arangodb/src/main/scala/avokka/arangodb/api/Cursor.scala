package avokka.arangodb
package api

import avokka.velocypack.VPack.VObject
import avokka.velocypack._

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
final case class Cursor[V, T](
    query: String,
    bindVars: V,
    batchSize: Option[Long] = None,
    cache: Option[Boolean] = None,
    count: Option[Boolean] = None,
    memoryLimit: Option[Long] = None,
    options: Option[Cursor.Options] = None,
    ttl: Option[Long] = None,
) {
  def withBatchSize(size: Long): Cursor[V, T] = copy(batchSize = Some(size))
  def withCache(flag: Boolean): Cursor[V, T] = copy(cache = Some(flag))
  def withCount(flag: Boolean): Cursor[V, T] = copy(count = Some(flag))
  def withMemoryLimit(bytes: Long): Cursor[V, T] = copy(memoryLimit = Some(bytes))
  def withTtl(seconds: Long): Cursor[V, T] = copy(ttl = Some(seconds))
}

object Cursor { self =>

  def apply[T](query: String): Cursor[VObject, T] = Cursor(query, VObject.empty)

  /**
    * key/value object with extra options for the query.
    *
    * @param failOnWarning When set to *true*, the query will throw an exception and abort instead of producing a warning. This option should be used during development to catch potential issues early. When the attribute is set to *false*, warnings will not be propagated to exceptions and will be returned with the query result. There is also a server configuration option `--query.fail-on-warning` for setting the default value for *failOnWarning* so it does not need to be set on a per-query level.
    * @param fullCount if set to *true* and the query contains a *LIMIT* clause, then the result will have an *extra* attribute with the sub-attributes *stats* and *fullCount*, `{ ... , \"extra\": { \"stats\": { \"fullCount\": 123 } } }`. The *fullCount* attribute will contain the number of documents in the result before the last top-level LIMIT in the query was applied. It can be used to count the number of  documents that match certain filter criteria, but only return a subset of them, in one go. It is thus similar to MySQL's *SQL_CALC_FOUND_ROWS* hint. Note that setting the option will disable a few LIMIT optimizations and may lead to more documents being processed, and thus make queries run longer. Note that the *fullCount* attribute may only be present in the result if the query has a top-level LIMIT clause and the LIMIT  clause is actually used in the query.
    * @param intermediateCommitCount Maximum number of operations after which an intermediate commit is performed automatically. Honored by the RocksDB storage engine only.
    * @param intermediateCommitSize Maximum total size of operations after which an intermediate commit is performed automatically. Honored by the RocksDB storage engine only.
    * @param maxPlans Limits the maximum number of plans that are created by the AQL query optimizer.
    * @param maxTransactionSize Transaction size limit in bytes. Honored by the RocksDB storage engine only.
    * @param maxWarningCount Limits the maximum number of warnings a query will return. The number of warnings a query will return is limited to 10 by default, but that number can be increased or decreased by setting this attribute.
    * @param optimizerRules A list of to-be-included or to-be-excluded optimizer rules can be put into this attribute, telling the optimizer to include or exclude specific rules. To disable a rule, prefix its name with a `-`, to enable a rule, prefix it with a `+`. There is also a pseudo-rule `all`, which will match all optimizer rules.
    * @param profile If set to *true* or *1*, then the additional query profiling information will be returned in the sub-attribute *profile* of the *extra* return attribute, if the query result is not served from the query cache. Set to *2* the query will include execution stats per query plan node in sub-attribute *stats.nodes* of the *extra* return attribute. Additionally the query plan is returned in the sub-attribute *extra.plan*.
    * @param satelliteSyncWait This *Enterprise Edition* parameter allows to configure how long a DBServer will have time to bring the satellite collections involved in the query into sync. The default value is *60.0* (seconds). When the max time has been reached the query will be stopped.
    * @param skipInaccessibleCollections AQL queries (especially graph traversals) will treat collection to which a user has no access rights as if these collections were empty. Instead of returning a forbidden access error, your queries will execute normally. This is intended to help with certain use-cases: A graph contains several collections and different users execute AQL queries on that graph. You can now naturally limit the accessible results by changing the access rights of users on collections. This feature is only available in the Enterprise Edition.
    * @param stream Specify *true* and the query will be executed in a **streaming** fashion. The query result is not stored on the server, but calculated on the fly. *Beware*: long-running queries will need to hold the collection locks for as long as the query cursor exists.  When set to *false* a query will be executed right away in its entirety.  In that case query results are either returned right away (if the result set is small enough), or stored on the arangod instance and accessible via the cursor API (with respect to the `ttl`).  It is advisable to *only* use this option on short-running queries or without exclusive locks  (write-locks on MMFiles). Please note that the query options `cache`, `count` and `fullCount` will not work on streaming queries. Additionally query statistics, warnings and profiling data will only be available after the query is finished. The default value is *false*
    */
  final case class Options(
      failOnWarning: Option[Boolean],
      fullCount: Option[Boolean],
      intermediateCommitCount: Option[Long],
      intermediateCommitSize: Option[Long],
      maxPlans: Option[Long],
      maxTransactionSize: Option[Long],
      maxWarningCount: Option[Long],
      optimizerRules: Option[List[String]],
      profile: Option[Int],
      satelliteSyncWait: Option[Boolean],
      skipInaccessibleCollections: Option[Boolean],
      stream: Option[Boolean],
  )

  object Options {
    implicit val encoder: VPackEncoder[Options] = VPackEncoder.gen
  }

  implicit def encoder[V: VPackEncoder, T]: VPackEncoder[Cursor[V, T]] = VPackEncoder.gen

  final case class ExtraStats(
      writesExecuted: Option[Long] = None,
      writesIgnored: Option[Long] = None,
      scannedFull: Option[Long] = None,
      scannedIndex: Option[Long] = None,
      filtered: Option[Long] = None,
      httpRequests: Option[Long] = None,
      fullCount: Option[Long] = None,
      executionTime: Option[Double] = None,
      peakMemoryUsage: Option[Long] = None
  )
  object ExtraStats {
    implicit val decoder: VPackDecoder[ExtraStats] = VPackDecoder.gen
  }

  final case class Extra(
      stats: ExtraStats
  )
  object Extra {
    implicit val decoder: VPackDecoder[Extra] = VPackDecoder.gen
  }

  /**
    * @param cached a boolean flag indicating whether the query result was served from the query cache or not. If the query result is served from the query cache, the *extra* return attribute will not contain any *stats* sub-attribute and no *profile* sub-attribute.
    * @param count the total number of result documents available (only available if the query was executed with the *count* attribute set)
    * @param extra an optional JSON object with extra information about the query result contained in its *stats* sub-attribute. For data-modification queries, the *extra.stats* sub-attribute will contain the number of modified documents and the number of documents that could not be modified due to an error (if *ignoreErrors* query option is specified)
    * @param hasMore A boolean indicator whether there are more results available for the cursor on the server
    * @param id id of temporary cursor created on the server (optional, see above)
    * @param result an array of result documents (might be empty if query has no results)
    */
  final case class Response[T](
      cached: Boolean,
      count: Option[Long] = None,
      extra: Option[Extra] = None,
      hasMore: Boolean,
      id: Option[String] = None,
      result: Vector[T]
  )

  object Response {
    implicit def decoder[T: VPackDecoder]: VPackDecoder[Response[T]] = VPackDecoder.gen
  }

  implicit def api[V: VPackEncoder, T: VPackDecoder]
    : Api.Command.Aux[ArangoDatabase, Cursor[V, T], Response[T]] =
    new Api.Command[ArangoDatabase, Cursor[V, T]] {
      override type Response = self.Response[T]
      override def header(database: ArangoDatabase,
                          command: Cursor[V, T]): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.POST,
          request = s"/_api/cursor"
        )
      override val encoder: VPackEncoder[Cursor[V, T]] = implicitly
    }
}
