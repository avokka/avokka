package avokka.arangodb
package models

import avokka.velocypack._

/**
  * @param cached a boolean flag indicating whether the query result was served from the query cache or not. If the query result is served from the query cache, the *extra* return attribute will not contain any *stats* sub-attribute and no *profile* sub-attribute.
  * @param count the total number of result documents available (only available if the query was executed with the *count* attribute set)
  * @param extra an optional JSON object with extra information about the query result contained in its *stats* sub-attribute. For data-modification queries, the *extra.stats* sub-attribute will contain the number of modified documents and the number of documents that could not be modified due to an error (if *ignoreErrors* query option is specified)
  * @param hasMore A boolean indicator whether there are more results available for the cursor on the server
  * @param id id of temporary cursor created on the server (optional, see above)
  * @param result an array of result documents (might be empty if query has no results)
  */
final case class Cursor[T](
    cached: Boolean,
    count: Option[Long] = None,
    extra: Option[Cursor.Extra] = None,
    hasMore: Boolean,
    id: Option[String] = None,
    result: Vector[T]
)

object Cursor {

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

  implicit def decoder[T: VPackDecoder]: VPackDecoder[Cursor[T]] = VPackDecoder.gen

}
