package avokka.arangodb

import avokka.velocystream.VStreamConfiguration
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig._
import pureconfig.generic.semiauto._
import types._

import scala.concurrent.duration.FiniteDuration

final case class ArangoConfiguration(
    host: String,
    port: Int = 8529,
    username: String,
    password: String,
    chunkLength: Long = VStreamConfiguration.CHUNK_LENGTH_DEFAULT,
    connectTimeout: FiniteDuration = VStreamConfiguration.CONNECT_TIMEOUT_DEFAULT,
    database: DatabaseName = DatabaseName.system
) extends VStreamConfiguration

object ArangoConfiguration {

  implicit val arangoDatabaseNameReader: ConfigReader[DatabaseName] = ConfigReader[String].map(DatabaseName.apply)

  implicit val arangoConfigurationReader: ConfigReader[ArangoConfiguration] = deriveReader

  def load(config: Config = ConfigFactory.load()): ArangoConfiguration =
    ConfigSource.fromConfig(config).at("avokka").loadOrThrow[ArangoConfiguration]
}
