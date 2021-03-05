package avokka.arangodb.aql

import avokka.arangodb.api.Query
import avokka.velocypack._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class AqlSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  it should "build a query" in {
    val q = aql"FOR doc IN collection RETURN d"

    q should be (a [Query[VObject]])
    q.bindVars should be (VObject.empty)
    q.query should be ("FOR doc IN collection RETURN d")
  }

  it should "interpolate bind vars" in {
    val attr = false
    val q = aql"FOR doc IN collection FILTER doc.attr = $attr RETURN d"

    q should be (a [Query[VObject]])
    q.bindVars.isEmpty should be (false)
    q.query should include ("@")
    q.query should include (s"@${q.bindVars.values.head._1}")
    q.bindVars.values.head._2 should be (VFalse)
  }

  it should "allow to manually bind var" in {
    val q = aql"FOR doc IN collection FILTER doc.attr = @attr RETURN d".bind("attr", true)

    q should be (a [Query[VObject]])
    q.bindVars.isEmpty should be (false)
    q.bindVars.values.head._2 should be (VTrue)
  }
}
