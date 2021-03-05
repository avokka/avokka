package avokka.arangodb

import api.Query
import avokka.velocypack._
import cats.syntax.align._

package object aql {

  implicit final class AqlStringContextOps(private val sc: StringContext) extends AnyVal {

    def aql(args: AqlBindVar*): Query[VObject] = {
      val placeholders = args.indices.map(i => s"@arg$i")
      val query = sc.parts.alignCombine(placeholders).mkString
      val bindVars = VObject(args.zipWithIndex.map {
        case (a, i) => s"arg$i" -> a.v
      }.toMap)
      Query(query, bindVars)
    }

  }

}
