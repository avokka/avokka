package avokka.arangodb.aql

import avokka.arangodb.models.Query
import avokka.arangodb.types.CollectionName
import avokka.velocypack._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class AqlSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  it should "build a query" in {
    val q = aql"FOR doc IN collection RETURN doc"

    q should be (a [Query[_]])
    q.bindVars should be (a [VObject])
    q.bindVars should be (VObject.empty)
    q.query should be ("FOR doc IN collection RETURN doc")
  }

  it should "interpolate bind vars" in {
    val attr = false
    val q = aql"FOR doc IN collection FILTER doc.attr = $attr RETURN doc"

    q should be (a [Query[_]])
    q.bindVars should be (a [VObject])
    q.bindVars.isEmpty should be (false)
    q.query should include ("@")
    q.query should include ("@" ++ q.bindVars.values.head._1)
    q.bindVars.values.head._2 should be (VFalse)
  }

  it should "interpolate collection name with double @" in {
    val coll = CollectionName("test")
    val q = aql"FOR doc IN $coll RETURN doc"

    q should be (a [Query[_]])
    q.bindVars should be (a [VObject])
    q.bindVars.isEmpty should be (false)
    q.query should include ("@@")
    q.query should include ("@" ++ q.bindVars.values.head._1)
    q.bindVars.values.head._2 should be (VString("test"))
  }

  it should "allow to manually bind var" in {
    val q = aql"FOR doc IN collection FILTER doc.attr = @attr RETURN doc".bind("attr", true)

    q should be (a [Query[_]])
    q.bindVars should be (a [VObject])
    q.bindVars.isEmpty should be (false)
    q.bindVars.values.head._2 should be (VTrue)
  }
}
