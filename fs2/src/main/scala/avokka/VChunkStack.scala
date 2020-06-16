package avokka

import scodec.bits.ByteVector

trait VChunkStack[F[_]] {
  def expected: Option[Long]
  def chunks: List[(VChunkHeader, ByteVector)]
}
