package avokka.velocystream

trait VStreamConfiguration {
  def host: String
  def port: Int
  def chunkLength: Long
}

object VStreamConfiguration {
  val CHUNK_LENGTH_DEFAULT: Long = 30000L
}