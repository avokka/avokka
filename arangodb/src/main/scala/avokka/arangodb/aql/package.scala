package avokka.arangodb

import api.Query
import avokka.velocypack._
import cats.syntax.align._

package object aql {

  implicit final class AqlStringContextOps(private val sc: StringContext) extends AnyVal {

    /**
      * tranforms a string with placeholders to an arango query with bound vpack values
      * @param args vpack bound arguments
      * @return arango query
      */
    def aql(args: AqlBindVar*): Query[VObject] = {
      // indexed variables with _arg$N key
      val bindVars = args.toVector.zipWithIndex.map { case (arg, i) => "_arg" + i -> arg.value }
      // placeholders with @ prefix
      val placeholders = bindVars.map { case (k, _) => "@" + k }
      // combine strings and placeholders to form query string
      val query = sc.parts.toVector.alignCombine(placeholders).mkString
      // query with bound variables
      Query(query, VObject(bindVars.toMap))
    }

  }

}
