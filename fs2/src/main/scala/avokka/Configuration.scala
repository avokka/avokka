package avokka

import pureconfig._
import pureconfig.generic.semiauto._

final case class Configuration(
    host: String = "localhost",
    port: Int = 8529,
    chunkLength: Long = 30000L,
    readBufferSize: Int = 256 * 1024
)

object Configuration {
  implicit val reader: ConfigReader[Configuration] = deriveReader
}
