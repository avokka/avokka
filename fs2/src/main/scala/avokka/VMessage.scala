package avokka

import avokka.velocystream.VStreamChunkX
import scodec.bits.ByteVector

/** complete velocystream payload
  *
  * @param id unique identifier
  * @param data payload
  */
final case class VMessage(
    id: Long,
    data: ByteVector
) {

  /** build the first chunk with all data
    *
    * @param length maximum length in bytes of chunks
    * @return first full chunk
    */
  def firstChunk(length: Long): VChunk = {
    val chunksCount = (data.size.toDouble / length).ceil.toLong
    val header = VChunkHeader(VStreamChunkX(first = true, chunksCount), id, data.size)
    VChunk(header, data)
  }
}
