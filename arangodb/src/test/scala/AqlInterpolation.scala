import avokka.arangodb.api.Query
import avokka.velocypack._
import cats.syntax.align._

object AqlInterpolation {

  trait VPackBind {
    def v: VPack
  }
  object VPackBind {
    implicit def from[T](t: T)(implicit e: VPackEncoder[T]): VPackBind = new VPackBind {
      override val v: VPack = e.encode(t)
    }
  }

  implicit final class AqlSC(private val sc: StringContext) extends AnyVal {
    def aql(args: VPackBind*): Query[VObject] = {
      val placeholders = args.indices.map(i => s"@arg$i")
      val query = sc.parts.alignCombine(placeholders).mkString
      val bindVars = VObject(args.zipWithIndex.map {
        case (packed, i) => s"arg$i" -> packed.v
      }.toMap)
      Query(query, bindVars)
    }
  }

  def main(args: Array[String]): Unit = {
    val ok = false
    val key = "azer"
    val q = aql"FOR d IN countries FILTER d._key = $key AND d.ok = $ok RETURN d"
    println(q)
    println()
    val q0 = aql"FOR d IN countries RETURN d"
    println(q0)
    println()
    val qi = aql"FOR d IN countries FILTER d.id = ${1 + 2} RETURN d"
    println(qi)
  }
}
