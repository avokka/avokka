package avokka

package object arangodb {

  private[avokka] implicit final class AvokkaStringMapUtilsOps(
      private val map: Map[String, Option[String]]
  ) extends AnyVal {
    def collectDefined: Map[String, String] = map.collect {
      case (key, Some(value)) => key -> value
    }
  }

  private[avokka] val DELETE = protocol.ArangoRequest.DELETE
  private[avokka] val GET = protocol.ArangoRequest.GET
  private[avokka] val POST = protocol.ArangoRequest.POST
  private[avokka] val PUT = protocol.ArangoRequest.PUT
  private[avokka] val HEAD = protocol.ArangoRequest.HEAD
  private[avokka] val PATCH = protocol.ArangoRequest.PATCH
  private[avokka] val OPTIONS = protocol.ArangoRequest.OPTIONS

  private[avokka] val API_DATABASE = "/_api/database"
  private[avokka] val API_COLLECTION = "/_api/collection"
  private[avokka] val API_DOCUMENT = "/_api/document"
  private[avokka] val API_INDEX = "/_api/index"
  private[avokka] val API_CURSOR = "/_api/cursor"
  private[avokka] val API_TRANSACTION = "/_api/transaction"
  private[avokka] val API_GHARIAL = "/_api/gharial"

}
