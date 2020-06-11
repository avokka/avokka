package avokka

final case class Configuration (
                                 host: String = "localhost",
                                 port: Int = 8529,
                                 chunkLength: Long = 30000L,
                                 readBufferSize: Int = 256 * 1024
                               )

