package avokka.arangodb.syntax

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

class StringContextOps(sc: StringContext) {
  def aql(argSeq: Any*): Any = macro StringContextOps.StringOpsMacros.aql_impl
}

object StringContextOps {
  class StringOpsMacros(val c: whitebox.Context) {
    import c.universe._

    def aql_impl(argSeq: Tree*): Tree = {

      val parts: List[Tree] =
        c.prefix.tree match {
          case Apply(_, List(Apply(_, ts))) => ts
          case _ => c.abort(c.prefix.tree.pos, "Unexpected tree, oops. See StringContextOps.scala")
        }

      val args = argSeq.toList

      val (finalParts, encoders) : (List[Tree /* part */], List[Tree] /* encoder */) =
        (parts zip args).foldRight((List(q"_root_.avokka.arangodb.syntax.StringContextOps.Str(${parts.last})"), List.empty[Tree])) {

              /*
          case ((part@Literal(Constant(str: String)), arg), (tail, es)) =>
            val argType = c.typecheck(arg, c.TYPEmode).tpe
               */

          case ((p, _), _) =>
            c.abort(p.pos, s"StringContext parts must be string literals.")
        }

      val finalEncoder: Tree =
        encoders.reduceLeftOption((a, b) => q"$a ~ $b").getOrElse(q"_root_.avokka.arangodb.Void.codec")

      q"_root_.avokka.arangodb.syntax.StringContextOps.fragmentFromParts($finalParts, $finalEncoder)"
    }
  }
}

trait ToStringContextOps {
  implicit def toStringOps(sc: StringContext): StringContextOps =
    new StringContextOps(sc)
}

object stringcontext extends ToStringContextOps