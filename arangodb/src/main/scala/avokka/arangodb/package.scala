package avokka

package object arangodb {

  private[avokka] implicit final class AvokkaStringMapUtilsOps(
      private val map: Map[String, Option[String]]
  ) extends AnyVal {
    def collectDefined: Map[String, String] = map.collect {
      case (key, Some(value)) => key -> value
    }
  }

}
