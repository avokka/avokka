package avokka.arangodb

import com.typesafe.config.{Config, ConfigFactory}
import pureconfig._
import pureconfig.generic.semiauto._

case class ArangoConfiguration(
    host: String,
    port: Int = 8529,
    username: String,
    password: String,
    queueSize: Int = 100,
    database: String = "_system"
)

object ArangoConfiguration {

  implicit val arangoConfigurationReader: ConfigReader[ArangoConfiguration] = deriveReader

  def apply(): ArangoConfiguration = apply(ConfigFactory.load())

  def apply(config: Config): ArangoConfiguration =
    ConfigSource.fromConfig(config).at("avokka").loadOrThrow[ArangoConfiguration]
}
