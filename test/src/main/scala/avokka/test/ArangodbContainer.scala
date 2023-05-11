package avokka.test

import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.types.DatabaseName
import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy

import scala.util.Random

class ArangodbContainer(val password: String,
                        val version: String,
                        underlying: GenericContainer)
    extends GenericContainer(underlying) {

  import ArangodbContainer._

  def configuration: ArangoConfiguration = ArangoConfiguration(
    host = containerIpAddress,
    port = mappedPort(port),
    username = "root",
    password = password,
    database = DatabaseName("test")
  )

  def endpoint: String = "http://%s:%d".format(containerIpAddress, mappedPort(port))
}

object ArangodbContainer {

  val port: Int = 8529

  object Defaults {
    val version: String = System.getProperty("test.arangodb.version", "3.10.6")
    val password: String = Random.nextLong().toHexString
  }

  // In the container definition you need to describe, how your container will be constructed:
  case class Def(password: String = Defaults.password,
                 version: String = Defaults.version)
      extends GenericContainer.Def[ArangodbContainer](
        new ArangodbContainer(
          password,
          version,
          GenericContainer(
            dockerImage = s"arangodb:$version",
            env = Map("ARANGO_ROOT_PASSWORD" -> password),
            exposedPorts = Seq(port),
            classpathResourceMapping = Seq(
              ("docker-initdb.d/", "/docker-entrypoint-initdb.d/", BindMode.READ_ONLY)
            ),
            waitStrategy = (new HttpWaitStrategy)
              .forPath("/_db/test/_api/collection/countries")
              .withBasicCredentials("root", password)
          )
        )
      )
}
