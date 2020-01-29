package avokka.arangodb

import avokka.velocystream.VStreamConfiguration
import com.typesafe.config.Config
import pureconfig._
import pureconfig.generic.semiauto._

import scala.concurrent.duration.FiniteDuration

case class ArangoConfiguration(
    host: String,
    port: Int = 8529,
    username: String,
    password: String,
    chunkLength: Long = VStreamConfiguration.CHUNK_LENGTH_DEFAULT,
    connectTimeout: FiniteDuration = VStreamConfiguration.CONNECT_TIMEOUT_DEFAULT,
    database: String = "_system"
) extends VStreamConfiguration

object ArangoConfiguration {

  implicit val arangoConfigurationReader: ConfigReader[ArangoConfiguration] = deriveReader

  def apply(config: Config): ArangoConfiguration =
    ConfigSource.fromConfig(config).at("avokka").loadOrThrow[ArangoConfiguration]
}
