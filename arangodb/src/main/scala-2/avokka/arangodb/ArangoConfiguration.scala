package avokka.arangodb

import avokka.arangodb.types.DatabaseName
import avokka.velocystream.VStreamConfiguration
import pureconfig._
import pureconfig.generic.semiauto._
// import pureconfig.generic.derivation.default._

import scala.concurrent.duration.FiniteDuration

/**
  * arango configuration
  * @param host server hostname
  * @param port server port
  * @param username username
  * @param password password
  * @param chunkLength chunk byte size
  * @param connectTimeout timeout to connection
  * @param replyTimeout timeout to response
  * @param database default database name
  */
final case class ArangoConfiguration(
    host: String,
    port: Int = 8529,
    username: String,
    password: String,
    chunkLength: Long = VStreamConfiguration.CHUNK_LENGTH_DEFAULT,
    readBufferSize: Int = VStreamConfiguration.READ_BUFFER_SIZE_DEFAULT,
    connectTimeout: FiniteDuration = VStreamConfiguration.CONNECT_TIMEOUT_DEFAULT,
    replyTimeout: FiniteDuration = VStreamConfiguration.REPLY_TIMEOUT_DEFAULT,
    database: DatabaseName = DatabaseName.system
) extends VStreamConfiguration

object ArangoConfiguration {

  implicit val arangoDatabaseNameReader: ConfigReader[DatabaseName] = ConfigReader[String].map(DatabaseName.apply)

  implicit val arangoConfigurationReader: ConfigReader[ArangoConfiguration] = deriveReader

  def load(conf: ConfigSource = ConfigSource.default): ArangoConfiguration = at(conf).loadOrThrow[ArangoConfiguration]

  def at(conf: ConfigSource = ConfigSource.default): ConfigSource = conf.at("avokka")
}
