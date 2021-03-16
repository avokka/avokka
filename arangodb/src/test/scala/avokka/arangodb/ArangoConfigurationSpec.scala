package avokka.arangodb

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pureconfig.ConfigSource

class ArangoConfigurationSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  it should "load from default config source" in {
    val conf = ArangoConfiguration.load()

    conf.host should be ("localhost")
    conf.port should be (8529)
  }

  it should "load from pureconfig configsource" in {
    val conf = ArangoConfiguration.load(ConfigSource.string(
      """
        |avokka {
        | host: "testhost"
        | port: 1
        |}
        |""".stripMargin).withFallback(ConfigSource.defaultReference))

    conf.host should be ("testhost")
    conf.port should be (1)
  }
}
