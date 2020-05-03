package avokka.arangodb

import com.dimafeng.testcontainers.GenericContainer

import scala.util.Random

class ArangodbContainer(port: Int, val password: String, val version: String, underlying: GenericContainer)
    extends GenericContainer(underlying) {

  def configuration: ArangoConfiguration = ArangoConfiguration(
    host = containerIpAddress,
    port = mappedPort(port),
    username = "root",
    password = password
  )
}

object ArangodbContainer {

  object Defaults {
    val version: String = "3.6.3"
    val port: Int = 8529
    val password: String = Random.nextLong().toHexString
  }

  // In the container definition you need to describe, how your container will be constructed:
  case class Def(port: Int = Defaults.port, password: String = Defaults.password, version: String = Defaults.version)
      extends GenericContainer.Def[ArangodbContainer](
        new ArangodbContainer(
          port,
          password,
          version,
          GenericContainer(
            dockerImage = s"arangodb:$version",
            env = Map("ARANGO_ROOT_PASSWORD" -> password),
            exposedPorts = Seq(port),
          )
        )
      )
}
