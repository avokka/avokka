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

}
