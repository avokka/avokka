package avokka.arangodb

import avokka.velocystream.VStreamConfiguration
import com.typesafe.config.Config
import pureconfig._
import pureconfig.generic.semiauto._

case class ArangoConfiguration(
    host: String,
    port: Int = 8529,
    username: String,
    password: String,
    queueSize: Int = VStreamConfiguration.QUEUE_SIZE_DEFAULT,
    chunkLength: Long = VStreamConfiguration.CHUNK_LENGTH_DEFAULT,
    database: String = "_system"
) extends VStreamConfiguration

object ArangoConfiguration {

  implicit val arangoConfigurationReader: ConfigReader[ArangoConfiguration] = deriveReader

  def apply(config: Config): ArangoConfiguration =
    ConfigSource.fromConfig(config).at("avokka").loadOrThrow[ArangoConfiguration]
}