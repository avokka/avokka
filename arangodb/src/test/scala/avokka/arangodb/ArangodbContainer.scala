package avokka.arangodb

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.BindMode

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
    password = password
  )

  def endpoint: String = "http://%s:%d".format(containerIpAddress, mappedPort(port))
}

object ArangodbContainer {

  val port: Int = 8529

  object Defaults {
    val version: String = "3.6.3"
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
            )
          )
        )
      )
}
