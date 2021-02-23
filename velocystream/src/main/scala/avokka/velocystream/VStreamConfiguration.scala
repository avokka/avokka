package avokka.velocystream

import scala.concurrent.duration._

trait VStreamConfiguration {
  def host: String
  def port: Int
  def connectTimeout: FiniteDuration
  def chunkLength: Long
  def readBufferSize: Int
  def replyTimeout: FiniteDuration
}

object VStreamConfiguration {
  val CHUNK_LENGTH_DEFAULT: Long = 30000L
  val READ_BUFFER_SIZE_DEFAULT: Int = 256 * 1024
  val CONNECT_TIMEOUT_DEFAULT: FiniteDuration = 10.seconds
  val REPLY_TIMEOUT_DEFAULT: FiniteDuration = 30.seconds
}