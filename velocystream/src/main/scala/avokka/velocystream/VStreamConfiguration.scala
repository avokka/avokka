package avokka.velocystream

import scala.concurrent.duration._

trait VStreamConfiguration {
  def host: String
  def port: Int
  def connectTimeout: FiniteDuration
  def chunkLength: Long
}

object VStreamConfiguration {
  val CHUNK_LENGTH_DEFAULT: Long = 30000L
  val CONNECT_TIMEOUT_DEFAULT: FiniteDuration = 10.seconds
}