package avokka.velocystream

import cats.Show
import cats.syntax.show._
import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs.{bytes, fixedSizeBytes, uint32L}
import scodec.interop.cats._

trait VStreamChunkCodec { self: VStreamChunk.type =>
  val codec: Codec[VStreamChunk] = uint32L.consume { l =>
    VStreamChunkHeader.codec :: fixedSizeBytes(l - dataOffset, bytes)
  } {
    case (_, bv) => bv.size + dataOffset
  }.as
}
