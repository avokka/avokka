package avokka.velocystream

trait VStreamConfiguration {
  def host: String
  def port: Int
  def queueSize: Int
  def chunkLength: Long
}

object VStreamConfiguration {
  val QUEUE_SIZE_DEFAULT: Int = 100
  val CHUNK_LENGTH_DEFAULT: Long = 30000L
}